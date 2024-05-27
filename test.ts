// 模拟用户数据库
const users = [
  { username: 'user1', password: 'password1' },
  { username: 'user2', password: 'password2' },
  // 更多用户...
];
// 模拟登录函数
function login(username, password) {
  const user = users.find(user => user.username === username && user.password === password);
  if (user) {
    console.log('登录成功');
  } else {
    console.log('用户名或密码错误');
  }
}
// 测试登录
login('user1', 'password1');
