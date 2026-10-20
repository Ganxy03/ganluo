# v1.0.17  发布说明

**code:** 18 / **name:** 1.0.17 / **commit:** 91e82d5d48f099f21b581245888ae24261286f43  / **branch:** android

## 用户能看到的改变
- 整体 UI 改造:底部 2 tab (记录 / 我的),告别"网页版移动端"
- 「位置共享」默认开启,**不在 UI 里加开关**(产品要求);sub-page 里可手动停
- 顶栏从 48dp 收到 44dp + hairline 分隔线,显示更紧凑
- 「我的」主页改成 iOS 风格:粉色 Header + 邀请码卡 + 绑定卡 + 分组列表 (位置共享 / 查看地图 / 蓝牙设备,检查更新 / 关于吉壹 / 退出登录)

## 修复/实现
- 真正实现位置共享:Service 加权限检查 + 进程内 ref count + 省电文案
- 自动开启 Service:App.onCreate 检测到 token / LoginViewModel.login 成功 / AppRoot LaunchedEffect(hasToken)
- 删掉右侧抽屉 (ui/nav/RightSideDrawer.kt),后台不再有那段死代码
- 我的子页改用嵌套 NavHost:{root, location, map, bluetooth, about},以后加功能加路由就行

## 验证
- ./gradlew :app:assembleDebug  → BUILD SUCCESSFUL (66.6 MB)
- ./gradlew :app:assembleRelease → BUILD SUCCESSFUL (53.4 MB,unsigned,自签名后分发)
- 模拟器 (API 35) 截图通过:Login → Home (心情日记 + 空态 + 卡片) → 我的 (改后新版) → 关于吉壹 → 位置共享 (未运行/开启按钮)
- 无 FATAL / AndroidRuntime 错误

## 产物
- D:\Work\ganluo-android\app\build\outputs\apk\debug\app-debug.apk
- D:\Work\ganluo-android\app\build\outputs\apk\release\app-release.apk

## 发布步骤
1. 把 tag 推到远端: git push origin v1.0.17 (沙箱网络不通,你那边推)
2. git push origin android 推分支
3. release: 把 app-release.apk 放到 https://y.bobay.top/public/downloads/ganluo-1.0.17.apk (你本地的签名版本)
4. (可选)发 GitHub release,贴上面这段说明
