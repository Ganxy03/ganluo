# 吉壹

> 情侣私密日常记录小程序 —— 心情日记 · 三餐报备 · 留言提醒

一个只服务于两个人的私密记录工具：微信一键登录，输入对方用户ID互相绑定，
之后就能看到彼此每天的心情、一日三餐，以及留给对方的话。

---

## 目录结构

```
ganluo/
├── docs/
│   └── 需求文档.md          ← 先看这个
├── ganluo/                  前端：uni-app（Vue3），用 HBuilderX 打开
│   ├── pages.json           页面注册 + TabBar 配置
│   ├── manifest.json        应用配置（微信 AppID 填这里）
│   ├── main.js / App.vue    入口 + 全局主题样式
│   ├── components/
│   │   └── InputSheet.vue   通用底部文字输入弹层
│   ├── pages/
│   │   ├── login/login.vue      登录 + 首次绑定
│   │   ├── index/index.vue      主页（日常）：我 / TA 两个板块
│   │   └── profile/profile.vue  个人页（我的）
│   └── utils/
│       ├── request.js       uni.request 封装（token 注入 / 401 跳登录）
│       ├── api.js           接口定义
│       ├── store.js         登录态（内存 + 本地缓存）
│       └── format.js        日期格式化
└── server/                  PHP 后端
    ├── public/index.php     入口（web 根目录）
    ├── public/uploads/      上传图片存放
    ├── routes/api.php       全部路由
    ├── src/                 框架 + 业务代码
    ├── sql/schema.sql       建表脚本
    └── .env.example         配置模板
```

---

## 环境要求

| 依赖 | 版本 |
|---|---|
| PHP | >= 8.1（已在 8.2.31 上验证） |
| MySQL | >= 5.7（已在 8.2 上验证） |
| **HBuilderX** | 最新稳定版（用于打开 / 编译 `ganluo`） |
| **微信开发者工具** | 最新稳定版（HBuilderX 会调用它来预览，**必须装**） |

后端**不需要** `composer install`，没有第三方依赖。

---

## 启动步骤

### 1. 建数据库

```bash
mysql -u root -p < server/sql/schema.sql
```

会创建数据库 `ganluo` 和 5 张表。

### 2. 配置后端

```bash
cp server/.env.example server/.env
```

编辑 `server/.env`，至少填 `DB_USERNAME` 和 `DB_PASSWORD`：

```ini
DB_DATABASE=ganluo
DB_USERNAME=root
DB_PASSWORD=你的密码
```

> `.env` 已在 `.gitignore` 里，不会被提交。

### 3. 启动后端

**双击 `server/start.bat`** 即可（Windows）。

或者用命令行：

```bash
php -S 127.0.0.1:8000 -t server/public
```

> ⚠️ **这个窗口要一直开着**。小程序报 `ERR_CONNECTION_REFUSED` 就是因为它没在跑。
> 端口必须和 `ganluo/utils/request.js` 里的 `BASE_URL` 一致（默认 8000）。

验证是否跑通：

```bash
curl http://127.0.0.1:8000/api/auth/me
# 预期：{"code":-1,"message":"未登录","data":null}
```



---

## 关于微信登录的真实限制

**游客模式 / 测试号下，`wx.login` 拿到的 code 无法在服务端换取真实 openid。**

要打通真实登录，需要：

1. 在 [微信公众平台](https://mp.weixin.qq.com) 注册小程序，拿到 `AppID` 和 `AppSecret`
2. 填进 `server/.env`：
   ```ini
   WECHAT_APPID=wx1234567890abcdef
   WECHAT_SECRET=your_app_secret
   ```
3. 把 `ganluo/manifest.json` 里 `mp-weixin.appid` 换成自己的

在此之前，前后端的**其他功能都无法联调**（因为拿不到 token）。

> 💡 如果只是想先看界面效果，我可以临时加一个"开发模式登录"开关，
> 跳过微信校验直接建号。需要的话告诉我。

---


## 相关文档

- 需求文档：`docs/需求文档.md`
- 建表脚本：`server/sql/schema.sql`
- 接口清单：需求文档 第六节
