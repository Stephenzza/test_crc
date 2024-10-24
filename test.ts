// 定义商品信息
let products = {
    "apple": 2.5,
    "banana": 1.5,
    "orange": 3.0
};

// 获取用户输入的商品和数量
let productName = prompt("请输入您要购买的商品名称（apple、banana、orange）：");
let quantity = parseInt(prompt("请输入您要购买的数量："));

// 检查用户输入是否有效
if (!(productName in products) || isNaN(quantity) || quantity <= 0) {
    console.log("请输入有效的商品名称和数量。");
} else {
    // 计算总价并输出
    let totalPrice = products[productName] * quantity;
    console.log(`您购买的商品总价为：$${totalPrice.toFixed(2)}`);
}
asssssssssssssssssssss
d
ad
a
a
d
