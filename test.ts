// 模拟投票系统
let votes = {}; // 存储投票数据的对象

// 函数：投票
function vote(voterId, candidate) {
    if (votes[voterId]) {
        console.log("您已经投过票了！");
        return;
    }
    votes[voterId] = candidate;
    console.log("您已成功投票给候选人", candidate);
}

// 函数：计票
function countVotes() {
    let result = {};
    for (let voterId in votes) {
        let candidate = votes[voterId];
        if (result[candidate]) {
            result[candidate]++;
        } else {
            result[candidate] = 1;
        }
    }
    console.log("投票结果：", result);
}

// 示例投票
vote("001", "候选人A");
vote("002", "候选人B");
vote("003", "候选人A");
vote("001", "候选人B"); // 注意这是一个重复投票

countVotes();
