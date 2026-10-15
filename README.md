# 吉壹 Android (ganluo-android)

> 把仓库 `Ganxy03/ganluo` 里的 `ganluo/` 小程序前端(uni-app Vue3)
> 重构为一个**可维护**的原生 Android APK。

后端复用 `https://y.bobay.top/`,不破坏现有 22 个端点和小程序用户,
只**新增** 2 个端点(登录 + 版本检查),具体见 [`server-patch/`](./server-patch/README.md)。

---

## 快速开始

### 真机/模拟器装 APK

```bash
adb install -r D:\Work\jiyi-debug.apk
```

应用名:**吉壹** · 包名:`cn.ganluo.jiyi.debug` · 大小:21 MB · minSdk 26 (Android 8.0)

### 自己出 APK

```powershell
cd D:\Work\ganluo-android
.\gradlew.bat assembleDebug
# 产物:app\build\outputs\apk\debug\app-debug.apk
```

要发正式版:

```powershell
.\gradlew.bat assembleRelease
# 产物:app\build\outputs\apk\release\app-release-unsigned.apk
# 自行签名后再分发
```

---

## 切换后端地址

`app/build.gradle.kts` 里改这一行,改完 `assembleDebug` 即可:

```kotlin
buildConfigField("String", "API_BASE_URL", "\"https://y.bobay.top\"")
```

可换成:

- 内网联调:`"http://192.168.x.x:8000"`
- 局域网 mock:`"http://10.0.2.2:8000"`(模拟器)
- 正式服:`"https://y.bobay.top"`

---

## 项目结构

```
ganluo-android/
├── app/
│   ├── build.gradle.kts                          # AGP 8.7.3 / Kotlin 2.0.21 / Compose
│   ├── src/main/
│   │   ├── AndroidManifest.xml                   # 权限 + FileProvider
│   │   ├── res/                                  # 主题色 #FF8FA3、图标、FileProvider 路径
│   │   └── java/com/ganluo/jiyi/
│   │       ├── App.kt                            # @HiltAndroidApp,启动检查更新
│   │       ├── MainActivity.kt                   # 单 Activity + ComposeView
│   │       ├── data/
│   │       │   ├── local/TokenStore.kt           # DataStore 存 JWT
│   │       │   ├── remote/
│   │       │   │   ├── ApiService.kt             # 24 个端点
│   │       │   │   ├── UnwrapConverterFactory.kt # {code,message,data} 自动解包
│   │       │   │   ├── AuthInterceptor.kt        # 自动塞 Authorization
│   │       │   │   └── dto/Dtos.kt
│   │       │   ├── repository/                   # Auth/Home/Mood/Message/Bind/Update/Upload
│   │       │   └── di/NetworkModule.kt           # Moshi/OkHttp/Retrofit 装配
│   │       ├── ui/
│   │       │   ├── theme/                        # 粉色 #FF8FA3
│   │       │   ├── nav/AppNavGraph.kt            # 单 NavHost + 底部 Tab
│   │       │   ├── login/                        # 邀请码登录(替代微信)
│   │       │   ├── home/                         # 日常页:心情 + 留言
│   │       │   ├── profile/                      # 我的:资料 + 绑定
│   │       │   ├── bluetooth/BluetoothScanScreen.kt
│   │       │   ├── component/                    # 通用组件
│   │       │   └── util/DateUtil.kt              # 移植自 utils/format.js
│   │       ├── permissions/                      # 权限注册表 + 请求器
│   │       ├── update/                           # 自建 OTA:FileProvider + OkHttp
│   │       └── bluetooth/                        # 原生 BluetoothLeScanner + callbackFlow
│   └── server-patch/                             # 后端增量(用户自己部署)
│       ├── README.md
│       ├── AuthAppController.php
│       ├── AppVersionController.php
│       ├── api_app.php
│       └── api.php.snippet
├── gradle/wrapper/                               # Gradle 8.10.2 (Tencent 镜像)
├── build.gradle.kts                              # root,plugin 版本锁死
├── settings.gradle.kts                           # Aliyun + Google + MavenCentral
├── gradle.properties
└── local.properties                              # sdk.dir(不提交)
```

---

## 技术选型

| 层 | 选型 | 说明 |
|---|---|---|
| 语言/UI | Kotlin 2.0.21 + Jetpack Compose (BOM 2024.10.01) | Material3 |
| 架构 | MVVM + Repository + Hilt 2.52 + KSP | 不引 kapt,构建快 30% |
| 网络 | Retrofit 2.11 + OkHttp 4.12 + Moshi 1.15.1 | 统一拦截 + 响应解包 |
| 异步 | Coroutines 1.9.0 + Flow | callbackFlow 桥接 BLE |
| 存储 | DataStore Preferences 1.1.1 | 替代原 uni.storage |
| 图片 | Coil 2.7.0 | Compose 集成 |
| 导航 | Navigation Compose 2.8.4 | 单 NavHost + 底部 Tab |
| 权限 | Accompanist Permissions 0.36 | 桥接 ActivityResultContracts |
| 更新 | 自建(FileProvider + OkHttp 进度 + ACTION_VIEW) | **不用 Play Core** |
| 蓝牙 | 原生 BluetoothLeScanner + callbackFlow | 不引第三方,接口在 `BleManager` |
| AGP / Gradle / JDK | 8.7.3 / 8.10.2 / 17 (Temurin) | |
| compileSdk / targetSdk | 35 (Android 15) | |
| minSdk | 26 (Android 8.0) | BLE 5.x、Adaptive Icons |

**镜像源**(中国网络友好):
- `settings.gradle.kts`: Aliyun Maven + Google + MavenCentral
- `gradle-wrapper.properties`: Tencent 镜像

---

## 关键模块

### 1. 登录流(端到端)

`LoginScreen` → `LoginViewModel.login(code)` → `AuthRepository.login(code)`
→ `Retrofit /api/auth/login-app`(跳过 AuthInterceptor)
→ 拿 JWT → `TokenStore.setToken(jwt)` → 拉 `/api/auth/me` 拿完整 user
→ 跳 Home(已绑 → Home,未绑 → Profile)

后端要求见 [`server-patch/`](./server-patch/README.md)。
**注意**:`AuthInterceptor` 自动给所有非登录/非版本端点加 `Authorization: Bearer <token>`,
`/api/auth/login-app` 和 `/api/app/version` 在 `skipAuth` 白名单里。

### 2. 自动更新流

`App.onCreate` → `AppUpdateChecker.checkOnLaunch()`
→ `GET /api/app/version`
→ `latest_code > BuildConfig.VERSION_CODE`?
→ 是 → `AppUpdateChecker.pending` → UI 弹 `UpdateSheet`
→ 用户点"立即更新" → `Downloader.download()` → `filesDir/updates/ganluo-v{code}.apk`
→ 100% → `ApkInstaller.install()`
→ `canRequestPackageInstalls()` 检查 + 请求权限
→ `FileProvider.getUriForFile()` + `ACTION_VIEW` + `GRANT_READ_URI_PERMISSION`
→ 系统安装器接管

发新版流程:
1. `.\gradlew.bat assembleRelease` 出新 APK
2. 上传到 `https://y.bobay.top/public/downloads/ganluo-{versionName}.apk`
3. 改 server `AppVersionController::latest()` 的 `latest_code` / `latest_name` / `download_url`
4. 旧 App 用户启动时会自动弹更新

### 3. BLE 流(v2 才用,v1 只搭骨架)

```
BluetoothScanScreen → PermissionRequester.ensure(SCAN_BLE)
  → 全同意 → BleManager.scan(timeout=8s)
    → Flow<ScanResult> → UI 列出设备
    → 用户点 → BleManager.connect(deviceId)  ← v2 实现
```

`BleManager` 是 interface,实现藏在 `BleManagerImpl`,以后换 Nordic 库不用动 UI。

### 4. 权限策略

`PermissionRegistry` 一张表:

| Key | 权限 |
|---|---|
| `SCAN_BLE` | `BLUETOOTH_SCAN` + `BLUETOOTH_CONNECT` (API 31+) / `ACCESS_FINE_LOCATION` (≤30) |
| `UPLOAD_IMAGE` | `READ_MEDIA_IMAGES` (API 33+) / `READ_EXTERNAL_STORAGE` (≤32) |
| `INSTALL_APK` | `REQUEST_INSTALL_PACKAGES` |

`PermissionRequester.ensure(SCAN_BLE)` 返回 Boolean,UI 据此 disable 按钮或弹 rationale。

---

## 复用了原前端的什么(对照表)

| 原文件 | 新位置 | 备注 |
|---|---|---|
| `utils/api.js` 22 端点 | `data/remote/ApiService.kt` | 方法签名 1:1,`uni.request` → Retrofit |
| `utils/request.js` token 注入 | `data/remote/AuthInterceptor.kt` | 401 自动跳登录,沿用 `redirectToLogin` 思路 |
| `utils/store.js` 登录态 | `data/local/TokenStore.kt` | DataStore 替代 uni.storage |
| `utils/format.js` 8 个工具函数 | `ui/util/DateUtil.kt` | `toDateStr`/`shiftDate`/`friendlyDate` 直接照搬 |
| `components/InputSheet.vue` 底部弹层 | `ui/component/InputSheet.kt` | Compose 写 |
| `App.vue` 主题色 `#FF8FA3` | `ui/theme/Color.kt` | Material3 ColorScheme |
| `index.vue` MOODS 8 种 + emoji | `ui/home/MoodSection.kt` | `MOODS = listOf(Mood("happy","开心","😊"),...)` |
| `index.vue` 心情/留言 UI | `ui/home/HomeScreen.kt` | 1:1 重写 |
| `profile.vue` 资料 + 绑定 | `ui/profile/ProfileScreen.kt` | 1:1 重写 |
| `login.vue` 微信登录 | `ui/login/LoginScreen.kt` | 邀请码输入,跟原注册流程对齐 |

Meals 模块**不在 v1 范围**(原代码 `v-if="false"` 已禁用,等用户决定是否恢复)。

---

## 后端需要做什么

见 [`server-patch/README.md`](./server-patch/README.md)。简短版:

1. 拷 2 个 controller 到 `server/src/Controllers/`
2. 拷 1 个路由文件到 `server/routes/`
3. 在 `server/routes/api.php` 末尾追加 1 行
4. 重启 PHP

不动现有任何一行代码。

---

## 真机验证(用户自己跑)

1. 装好 APK + 后端部署完成
2. 打开 App,登录页输任意 8 位邀请码(去 I/O/0/1)
   - 命中已有用户 → 直接进 Home
   - 没命中 → 自动建号,随机昵称"软软的小汤圆"等
3. 进 Home 选心情 / 写留言
4. 进 Profile 改昵称/签名
5. 进 Profile 的"绑定另一半"输对方邀请码
6. 另一台设备装同样 APK,用对方邀请码登录,看到彼此数据
7. 在另一台机器升 versionCode 后,旧 App 启动应弹更新 Sheet

---

## ⚠️ 已知限制 / 后续

- v1 **没启用** release 签名,只产 debug APK。要发正式版要补签名配置。
- BLE **只搭了骨架**,真实 GATT 连接/读写特征值要 v2 实现。
- `dev_app_<invite_code>` 当 openid 的 dev 登录模型上线后要换成微信开放平台 SDK。
- Hilt + Moshi 用 KSP,build 日志里 Moshi codegen 会提示 "Kapt deprecated",
  这是 Moshi 1.x 自带,跟 KSP 配置无关,可忽略。
- 只测了 Windows 上的 `assembleDebug`,没在 macOS/Linux 跑过。

---

## 改这个项目时容易踩的坑

1. **改 API 地址**:`app/build.gradle.kts` 里的 `API_BASE_URL`,改完必须重 build。
2. **加新 DTO**:`data/remote/dto/Dtos.kt`,字段名跟后端 JSON 保持 snake_case,
   `@Json(name = "user_id")` 这种映射用 Moshi 注解。
3. **加新端点**:`ApiService.kt` 加 `suspend fun` + `@POST/@GET` + 路径,
   如果不需鉴权,在 `AuthInterceptor.skipAuth` 里加路径关键字。
4. **加新页**:`ui/nav/Route.kt` 加常量,`AppNavGraph.kt` 加 `composable(route) { ... }`。
5. **加新权限**:`permissions/PermissionRegistry.kt` 加一组,UI 调 `PermissionRequester.ensure(...)`。
6. **改发版本流程**:别忘改 `server/src/Controllers/AppVersionController.php` 的 3 个字段 + 上传新 APK 到 `public/downloads/`。
