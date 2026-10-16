# Android Studio 调试速查

> 项目已配好,按需翻这条 note 就行。

---

## 0. 启动项目

```
Android Studio → Open → D:\Work\ganluo-android
```

等右下角 `Gradle sync finished`。

---

## 1. 切后端到本机(免去绕 CF / 改 DNS)

**默认**:`https://y.bobay.top`(线上)
**改本机**:编辑项目根目录的 `local.properties`,末尾追加一行:

```properties
# 模拟器看 PC,需要 PC 端跑 php -S 0.0.0.0:8000 -t server/public
API_BASE_URL=http://10.0.2.2:8000

# 真机走 LAN,把 192.168.x.x 换成你 PC 内网 IP(ipconfig 看)
API_BASE_URL=http://192.168.1.100:8000

# 直连本机(部分模拟器不识别 10.0.2.2 时备用)
API_BASE_URL=http://127.0.0.1:8000
```

之后重新 Run / Apply Changes 就走本机后端了。

**注意**:`local.properties` 在 `.gitignore` 里,不会进 git,改完只对你本机生效。

---

## 2. Logcat 看完整请求响应

OkHttp 已经配 `Level.BODY` + 隐藏 JWT:

```
Logcat 标签过滤框:OkHttp
```

输出长这样:
```
--> POST https://y.bobay.top/api/mood
Content-Type: application/json
Content-Length: 42

{"type":"happy","content":"今天心情不错"}
<-- 200 OK (123ms, 89-byte body)
{"code":0,"data":{"id":42,"moodType":"happy",...}}
```

想看错误响应:过滤 `okhttp.OkHttpClient` 或 `System.err`。

---

## 3. Composable 实时预览(@Preview)

任何带 `@Preview` 的 Composable,文件打开后右侧点 **`Split`** / **`Design`** 切到预览视图:
- 改代码 → ⌘/Ctrl+S 保存 → 预览**秒刷**
- 点预览右上角 ↻ 强制刷新
- 想真机/模拟器看:点 ▶ Run

**已有 Preview 的组件**:
- `ui/component/MessageBubble.kt` → 我发的 / TA 发的
- `ui/component/Avatar.kt` → 三种尺寸占位
- `ui/home/MoodSection.kt` → 心情选择弹窗、空记录

**自己加 Preview** 的最小模板:
```kotlin
@Preview(showBackground = true, name = "我的组件名")
@Composable
private fun PreviewMyScreen() {
    JiyiTheme {
        MyComposable(...)
    }
}
```

---

## 4. 断点调试

1. 代码行号左边**点一下** → 出现红点 = 断点
2. 工具栏点 🪲 **Debug 'app'** 按钮(虫子图标)
3. 跑 App → 到断点会停 → 看 Variables 窗口

**热键**:
| 操作 | Mac | Windows |
|---|---|---|
| Step Over(下一步) | F8 | F8 |
| Step Into(进方法) | F7 | F7 |
| Resume(继续) | F9 | F9 |
| Stop | ⌘F2 | Ctrl+F2 |
| 切换断点 | ⌘F8 | Ctrl+F8 |

**条件断点**:右键断点 → Condition → 写 `userId == 42`,只在该 userId 触发时停。

---

## 5. Apply Changes(改代码不重装)

跑起来后改了 Kotlin 代码:
- 工具栏 **⚡ Apply Changes** → 增量推到设备,**不重装不重启**
- 不行就 ▶ Run 重装一次

**Live Edit**(Android Studio Hedgehog+):改 Composable → ⌘/Ctrl+S → 真机**秒级**刷新(实验功能,Settings → Editor → Live Edit 开)。

---

## 6. 设备选择(顶部下拉框)

- 你的真机(USB 连上就在)
- 已建的模拟器(`More Options → AVD Manager → Create` 选 API 35 + arm64)
- 都没有 → `More Options → Pair Devices Using Wi-Fi` 配对手机

---

## 7. 常用面板速查

| 面板 | 干啥 | 打开方式 |
|---|---|---|
| Logcat | 实时日志 | 底部 Logcat 标签 |
| Profiler | CPU / 内存 / 网络 | View → Tool Windows → Profiler |
| Database Inspector | 看 DataStore / Room | View → Tool Windows → App Inspection |
| Layout Inspector | 看 UI 树 / padding | Tools → Layout Inspector(跑起来后) |
| TODO | 全部 `@Todo` 标记 | 底部 TODO 标签 |

---

## 8. 调试网络请求备选

- **App 内的 Logcat 过滤** `okhttp.OkHttpClient` → 最快
- **后端用 `php -S 0.0.0.0:8000 -t server/public`** → 终端直接看请求日志
- **Charles / Fiddler**(需配 proxy)→ 看 HTTPS 明文(要装证书)

---

## 9. 常用构建命令

```bash
cd D:\Work\ganluo-android
JAVA_HOME="/d/Env/jdk/jdk-17.0.13+11" ./gradlew.bat assembleDebug    # 出 debug APK
./gradlew.bat assembleRelease  # 出 release APK(需 ~/.gradle/gradle.properties 有 keystore)
./gradlew.bat --offline assembleDebug  # 不联网构建(快)
./gradlew.bat tasks             # 看所有任务
```

---

## 10. 出问题先看这些

| 现象 | 看啥 |
|---|---|
| Run 不见设备 | `adb devices`(确认 USB 调试开了) |
| 装了闪退 | Logcat 过滤 `AndroidRuntime` |
| 网络 404/500 | Logcat 过滤 `OkHttp` 看 body |
| Compose 不刷新 | Tools → Layout Inspector 看 recompose 计数 |
| Gradle sync 失败 | Build → Clean Project,再 Sync |
