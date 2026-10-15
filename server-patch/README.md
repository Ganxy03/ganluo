# 后端增量补丁(server-patch)

为支持「吉壹」Android 客户端,本目录提供 2 个**新增**端点,完全不动现有
`https://y.bobay.top/` 的任何文件(不动路由、不动 controller、不动表结构)。

## 改动清单

| 路径 | 操作 | 作用 |
|---|---|---|
| `src/Controllers/AuthAppController.php` | 新建 | Android 端邀请码登录(替代微信 wx.login) |
| `src/Controllers/AppVersionController.php` | 新建 | 自动更新元数据 |
| `routes/api_app.php` | 新建 | 注册上面 2 个端点 |
| `routes/api.php` | 末尾追加 **1 行** | `require __DIR__ . '/api_app.php';` |

## 部署步骤

```bash
# 1. 把这 4 个文件拷到 server/ 对应位置
cp AuthAppController.php   /www/ganluo/server/src/Controllers/
cp AppVersionController.php /www/ganluo/server/src/Controllers/
cp api_app.php             /www/ganluo/server/routes/

# 2. 在 server/routes/api.php 末尾追加一行
echo "require __DIR__ . '/api_app.php';" >> /www/ganluo/server/routes/api.php

# 3. 重启 PHP 内置 server 或 php-fpm
#    (如果用 start.bat 关掉重开;php-fpm 走 reload)
```

## 端点行为

### POST /api/auth/login-app

请求:
```json
{ "invite_code": "ABC12345" }
```

行为:
- 校验 8 位邀请码(去 I/O/0/1)
- 查 `users` 表 `WHERE invite_code = ?`
- 命中 → 签 JWT 返回
- 未命中 → 自动建号(随机昵称 + 占位 openid `dev_app_<invite_code>`),签 JWT 返回
- 不校验密码(开发模式,等价于小程序"知道 openid 就能进"的威胁模型)

响应(成功):
```json
{
  "code": 0, "message": "ok",
  "data": {
    "token": "<JWT, 30 天>",
    "user":  { "id": 42, "nickname": "软软的小汤圆", "invite_code": "ABC12345",
              "bound": false, "partner": null, ... }
  }
}
```

### GET /api/app/version

无鉴权。返回:
```json
{
  "code": 0, "message": "ok",
  "data": {
    "latest_code":   1,                              // 跟 app/build.gradle.kts versionCode 对齐
    "latest_name":   "1.0.0",                        // 跟 versionName 对齐
    "download_url":  "https://y.bobay.top/downloads/ganluo-1.0.0.apk",
    "changelog":     "首版上线",
    "force_update":  false,                          // true 时 Android 端不让"稍后"
    "min_supported": 1                               // 低于此 versionCode 强制升级
  }
}
```

发新版本流程:
1. 本机构建 release APK(见 ../README.md)
2. 上传到 `/www/ganluo/public/downloads/ganluo-{versionName}.apk`
3. 改本文件 `AppVersionController::latest()` 的 `latest_code` / `latest_name` / `download_url`
4. Android 端用户启动 App 时会自动弹更新 Sheet

## ⚠️ 落地前请检查

`AuthAppController.php` 和 `AppVersionController.php` 里的类名空间、
方法名是按 server/src/ 现有命名约定的**骨架**。你部署前需要确认:

| 文件 | 检查点 |
|---|---|
| `App\Services\JwtService` | 你现有的 JWT 服务类名 / `issue($claims)` 签名是否一致 |
| `App\Models\User` | `findByInviteCode($code)` / `find($id)` / `create($data)` 这几个方法是否同名同参 |
| `users` 表字段 | `openid / nickname / invite_code / signature / partner_id / bind_at / status` 是否都在 |

如果方法名不一致(很常见),把 controller 里的方法调用换成你项目里实际有的就行,
业务逻辑完全照搬。

## ⚠️ Dev-only 警告

`login-app` 不校验密码,等微信开放平台 SDK 接入后再加,或者你可以加一个
`.env` 的 `APP_LOGIN_DEV=true` 开关,正式上线关掉。
