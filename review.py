from langchain_tornado.utils.loguru_log import LOG
from langchain_tornado.utils.code_review_strict.issue_analysis import (
    create_issue_analysis_chain,
)
from langchain_tornado.utils.code_review_strict.extract import (
    create_extract_chain,
    ISSUE_EXTRACT_PROMPT,
    ISSUE_SCHEMA,
    CHECK_EXTRACT_PROMPT,
    CHECK_SCHEMA,
    KEEP_EXTRACT_PROMPT,
    KEEP_SCHEMA,
    DESCRIPTION_EXTRACT_PROMPT,
    DESCRIPTION_SCHEMA
)
from langchain_tornado.utils.code_review_strict.issue_check import (
    create_issue_check_chain,
)
from langchain_tornado.utils.code_review_strict.transform import create_transform_chain
from langchain_tornado.utils.code_review_strict.code_refine import (
    create_code_refine_chain,
)
from langchain_tornado.utils.code_review_strict.issue_deduplicate import (
    create_issue_deduplicate_chain,
)

from langchain_tornado.utils.code_review_strict.create_llm import create_llm

from langchain.callbacks.manager import get_openai_callback
from langchain_tornado.utils.code_review_strict.data import Issue, LineRange, GerritMap
from langchain.globals import set_debug

set_debug(True)


async def code_review(each_content, checklist, retriever):

    gerrit_map = GerritMap(filename=each_content["fname"])
    llm = create_llm()
    issue_analysis_chain = create_issue_analysis_chain("issue_analysis", llm)
    issue_extract_chain = create_extract_chain(
        "issues", llm, ISSUE_SCHEMA, ISSUE_EXTRACT_PROMPT
    )
    check_extract_chain = create_extract_chain(
        "checks", llm, CHECK_SCHEMA, CHECK_EXTRACT_PROMPT
    )
    deduplicate_extract_chain = create_extract_chain(
        "checks", llm, KEEP_SCHEMA, KEEP_EXTRACT_PROMPT
    )
    desc_extract_chain = create_extract_chain(
        "descriptions", llm, DESCRIPTION_SCHEMA, DESCRIPTION_EXTRACT_PROMPT
    )

    issue_check_chain = create_issue_check_chain(retriever, llm, max_iterations=3)
    generate_diff_transform = create_transform_chain(
        ["each_content"], ["diff"], "generate_diff"
    )
    deduplicate_patch_transform = create_transform_chain(
        ["each_content"], ["deduplicate_patch"], "deduplicate_patch"
    )
    deduplicate_issue_transform = create_transform_chain(
        ["filtered_issue"], ["deduplicated_issues"], "deduplicate_issue"
    )
    gerrit_patch_transform = create_transform_chain(
        ["issues"], ["gerrit_issues"], "gerrit_patch"
    )
    code_refine_patch_transform = create_transform_chain(
        ["each_content", "issue"], ["code_refine_patch"], "code_refine"
    )
    issue_check_transform = create_transform_chain(
        ["each_content", "issue"], ["issue_check_patch"], "issue_check"
    )
    code_refine = create_code_refine_chain("refined_code", llm)
    deduplicate_patch_chain = create_issue_deduplicate_chain("issue_deduplicate", llm)

    target_lang = each_content["code_lang"]
    batch_diff = generate_diff_transform.invoke({"each_content": each_content})["diff"]
    issues_with_code = []
    with get_openai_callback() as cb:
        for code_patch, supported_ranges in batch_diff:
            issues_chain = (
                issue_analysis_chain | issue_extract_chain | gerrit_patch_transform
            )
            issues = issues_chain.invoke(
                {
                    "code_patch": code_patch,
                    "checklist": checklist,
                    "target_lang": target_lang,
                }
            )["gerrit_issues"]
            check_patch = issue_check_transform.batch(
                [{"each_content": each_content, "issue": issue} for issue in issues]
            )

            check_chain = issue_check_chain | check_extract_chain
            check_results = check_chain.batch(
                [
                    {"input": patch["issue_check_patch"], "chat_history": []}
                    for patch in check_patch
                ]
            )
            check_issues = []
            for issue, check in zip(issues, check_results):
                try:
                    check_flag = check["checks"][0].get("keep", False)
                    if check_flag:
                        check_issues.append(issue)
                except (KeyError, IndexError, TypeError) as e:
                    LOG.error(f"An error occurred when check issue: {e}")


            code_refine_chain = code_refine_patch_transform | code_refine
            code_refined = code_refine_chain.batch(
                [
                    {"each_content": each_content, "issue": issue}
                    for issue in check_issues
                ]
            )

        for issue, refined_code in zip(check_issues, code_refined):
            issue_copy = issue.copy()
            issue_copy["refined_code"] = refined_code["refined_code"]
            issues_with_code.append(issue_copy)

        result = []
        if issues_with_code:
            deduplicate_chain = (
                deduplicate_issue_transform
                | deduplicate_patch_transform
                | deduplicate_patch_chain
                | deduplicate_extract_chain
            )
            total_patches_num = len(issues_with_code)
            keep_ids = deduplicate_chain.invoke(
                {"each_content": each_content, "filtered_issue": issues_with_code}
            )
            keep_ids_list = []
            try:
                keep_ids_list = keep_ids["checks"][0].get("keep_ids", [])
            except (KeyError, IndexError, TypeError) as e:
                LOG.error(f"An error occurred when get deduplicated issue: {e}")
            result = [
                issues_with_code[i - 1] for i in keep_ids_list if 1 <= i <= total_patches_num
            ]
    descs = desc_extract_chain.batch([{"issue": issue["issue"]} for issue in result])

    patches = []
    for issue, desc in zip(result, descs):
        try:
            desc = desc["descriptions"][0]
            line_range = LineRange(
                start_line=issue["line_range"]["start_line"], end_line=issue["line_range"]["end_line"]
            )
            patch = Issue(
                line=issue["line_range"]["end_line"],
                line_range=line_range,
                issue=desc["issue"],
                issue_sim=desc["issue_sim"],
                suggestion=desc["suggestion"],
                suggestion_sim=desc["suggestion_sim"],
                comment=desc["suggestion"] + "\n" + issue["refined_code"],
                refined_code=issue["refined_code"],
            )
            patches.append(patch)
        except (KeyError, IndexError, TypeError) as e:
            LOG.error(f"An error occurred while processing issue {issue} and description {desc}: {e}")
    gerrit_map.patches = patches
    return gerrit_map.model_dump(), {
        "completion_tokens": cb.completion_tokens,
        "prompt_tokens": cb.prompt_tokens,
    }
