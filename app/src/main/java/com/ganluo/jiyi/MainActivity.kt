package com.ganluo.jiyi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.ganluo.jiyi.ui.nav.AppRoot
import com.ganluo.jiyi.ui.theme.JiyiTheme
import com.ganluo.jiyi.util.CrashLogger
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        // 面包屑:用来判断上个进程是不是死在这一步之后
        runCatching { CrashLogger.stage(this, "MainActivity.onCreate") }
        enableEdgeToEdge()
        setContent {
            JiyiTheme {
                // 仅 debug:首页排版预览(不登录、假数据),见 HomePreviewScreen
                if (BuildConfig.DEBUG && intent.getBooleanExtra("preview_home", false)) {
                    com.ganluo.jiyi.ui.home.HomePreviewScreen()
                } else {
                    AppRoot()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 正常退出(用户返回/完成)才标记收尾;被系统杀 / native 崩溃不会走到这里
        if (isFinishing) runCatching { CrashLogger.endSession(this) }
    }
}
