import re
import json
import random
from langchain_tornado.utils.loguru_log import LOG
from langchain_tornado.utils.Code_Review import TokenUsageTracker, remove_markdown_code_blocks
import difflib
def extract_numbers(text):
    text = str(text)
    text = re.findall(r"\d+", text)
    text = [int(num) for num in text]
    if text:
        return text[0]
    else:
        return -1
def generate_detailed_diff(text1, text2):
    lines1 = text1.splitlines()
    lines2 = text2.splitlines()
    diff = list(difflib.unified_diff(lines1, lines2, n=3, lineterm=""))
    lines_per_batch = 1000
    batches = []
    current_batch = []
    line_num1 = 0
    line_num2 = 0
    current_new_line_ranges = []
    start_new_line = None
    def process_batch():
        nonlocal current_batch, current_new_line_ranges, start_new_line, line_num2
        if start_new_line is not None:
            current_new_line_ranges.extend(
                list(range(start_new_line, line_num2 + 1))
            )
        batches.append(("\n".join(current_batch), current_new_line_ranges))
        current_batch = []
        current_new_line_ranges = []
        start_new_line = None
    for line in diff:
        if line.startswith("@@"):
            # 解析差异块的头部信息来获取正确的起始行号
            _, old_range, new_range, _ = line.split(" ", 3)
            line_num1 = int(old_range.split(",")[0][1:]) - 1
            line_num2 = int(new_range.split(",")[0][1:]) - 1
            continue
        if line.startswith("---") or line.startswith("+++"):
            continue
        if line.startswith(" "):  # 未修改的行
            if start_new_line is not None:
                current_new_line_ranges.extend(
                    list(range(start_new_line, line_num2 + 1))
                )
                start_new_line = None
            line_num1 += 1
            line_num2 += 1
            current_batch.append(f" {line[1:]}")
        elif line.startswith("-"):  # 删除的行
            if start_new_line is not None:
                current_new_line_ranges.append(f"{start_new_line}-{line_num2}")
                start_new_line = None
            line_num1 += 1
            current_batch.append(f"-{line[1:]}")
        elif line.startswith("+"):  # 新增的行
            line_num2 += 1
            if start_new_line is None:
                start_new_line = line_num2
            current_batch.append(f"+{line[1:]}")
        # 检查是否需要开始新的批次
        if len(current_batch) >= lines_per_batch:
            process_batch()
    # 处理最后一个批次
    if current_batch:
        process_batch()
    return batches
def parse_resolve_check_format(data):
    reviews = []
    review = {}
    for line in data.split("\n"):
        if "issueID" in line and review:
            reviews.append(review)
            review = {}
        if "<sep>" not in line:
            continue
        line_split = line.split("<sep>")
        if len(line_split) < 2:
            continue
        key, value = line_split[:2]
        key = re.findall(r"[a-zA-Z]+", key)
        if not key:
            continue
        else:
            key = key[0]
        review[key] = value
    if review:
        reviews.append(review)
    return reviews
async def resolve_check_chain(check_patch, code_lang, last_review, request_handler):
    refine_comment_ret = await request_handler.get_coder_chain_ret(
        user_question=check_patch, code_lang=code_lang, last_review=last_review
    )
    refine_comment_ret_json = json.loads(refine_comment_ret["answer"])
    try:
        refine_comment = refine_comment_ret_json["choices"][-1]["message"]["content"]
        # refine_comment = [choice["message"]["content"] for choice in refine_comment_ret_json["choices"]]
    except Exception as e:
        LOG.error(e)
        return False, refine_comment_ret_json
    return refine_comment, refine_comment_ret_json
async def resolve_check_process_chain(
    left_content, right_content, comments, request_handler
):
    token_tracker = TokenUsageTracker()
    resolve_results = []
    batch_diff = generate_detailed_diff(left_content, right_content)
    right_content_lines = right_content.split("\n")
    max_right_content_lines = len(right_content_lines)
    for code_diff, supported_ranges in batch_diff:
        unique_random_ids = set()
        while len(unique_random_ids) < len(comments):
            unique_random_ids.add(f"{random.randint(100, 999):03}")
        format_dict = {rids: c["id"] for rids, c in zip(unique_random_ids, comments)}
        code_ranges = set()
        for comment in comments:
            start_line = int(comment["range"]["start_line"])
            end_line = int(comment["range"]["end_line"])
            code_ranges.update(range(start_line-1, end_line)) 
        code_ranges = list(code_ranges)
        code_ranges.sort()
        zfill_length = len(str(max(code_ranges)))
        origin_code_patch = []
        for i in range(len(code_ranges)):
            if 0 <= code_ranges[i] < max_right_content_lines:
                origin_code_patch.append(f"{str(code_ranges[i]+1).zfill(zfill_length)}:{right_content_lines[code_ranges[i]]}")
            if i > 0 and code_ranges[i] - code_ranges[i-1] > 1:
                origin_code_patch.append(f'\n(some lines ommited)\n')
        origin_code_patch = '\n'.join(origin_code_patch)
        comments_refined = "\n".join(
            [f"[{i}]代码行号{c['range']['start_line']}-{c['range']['end_line']}；问题描述：{remove_markdown_code_blocks(c['message'])}" for i, c in zip(unique_random_ids, comments)]
        )
        last_review = origin_code_patch + '\n' + comments_refined
        code_lang = "python"
        resolve_check_result, resp_json = await resolve_check_chain(
            code_diff, code_lang, last_review, request_handler
        )
        token_tracker.update(resp_json)
        resolve_status = parse_resolve_check_format(resolve_check_result)
        for resolve in resolve_status:
            if resolve.get("resolve", "") == "是":
                random_id = resolve.get("issueID", "")
                random_id = extract_numbers(random_id)
                if random_id == -1:
                    continue
                resolve_results.append(format_dict[str(random_id)])
    return resolve_results, token_tracker.get_total()
