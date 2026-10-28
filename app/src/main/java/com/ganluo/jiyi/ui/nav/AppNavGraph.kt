package com.ganluo.jiyi.ui.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.ganluo.jiyi.permissions.LocationPermissionHelper
import com.ganluo.jiyi.ui.FeatureFlags
import com.ganluo.jiyi.ui.map.LocationShareService
import com.ganluo.jiyi.util.CrashLogger
import kotlinx.coroutines.delay
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ganluo.jiyi.ui.home.ChatScreen
import com.ganluo.jiyi.ui.home.HomeScreen
import com.ganluo.jiyi.ui.home.HomeViewModel
import com.ganluo.jiyi.ui.home.PartnerDetailScreen
import com.ganluo.jiyi.ui.login.LoginScreen
import com.ganluo.jiyi.ui.login.LoginViewModel
import com.ganluo.jiyi.ui.profile.ProfileScreen
import com.ganluo.jiyi.update.UpdateSheet

/**
 * 应用根(NavHost + 底部 nav):
 *  - 顶层路由:login / home(记录) / profile(我的) / chat / partner_detail
 *  - 「我的」内部嵌套一个 NavHost(Profile 自己的子图)
 *  - 主切换走底部 nav
 *  - 全局 UpdateSheet 放在 Scaffold 外层,所有 tab 都看得到
 *  - 有 token 时自动开启位置共享 Service(产品要求默认开启)
 *
 * ⚠️ HomeViewModel 必须由本函数(Activity 作用域)创建并向下传:
 *    如果各 composable 内各自 hiltViewModel(),会拿到按 NavBackStackEntry 划分的
 *    不同实例,聊天页那个实例从没 load() 过 → home==null → 误报「没有绑定对象」。
 */
@Composable
fun AppRoot(
    rootVm: AppRootViewModel = hiltViewModel()
) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    val loginVm: LoginViewModel = hiltViewModel()
    val hasToken by loginVm.hasToken.collectAsState(initial = null)

    // 共享 HomeViewModel(Activity 作用域):记录页 / 聊天窗 / TA 资料页共用一个实例
    val homeVm: HomeViewModel = hiltViewModel()
    val homeState by homeVm.home.collectAsStateWithLifecycle()
    val homeDate by homeVm.date.collectAsStateWithLifecycle()

    val startDest = when (hasToken) {
        true -> Route.HOME
        false, null -> Route.LOGIN
    }

    // 有 token 时:先确保定位权限,授予后再启动位置共享 Service
    // (没权限就启动 FGS 会在 Android 14 抛 SecurityException,所以必须走到这一步才启)
    val ctx = LocalContext.current
    val locPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result[android.Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                      result[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) runCatching { LocationShareService.start(ctx) }
    }
    LaunchedEffect(hasToken) {
        runCatching { CrashLogger.stage(ctx, "ui.ready token=$hasToken") }
        if (hasToken == true && FeatureFlags.LOCATION) {
            if (LocationShareService.hasLocationPermission(ctx)) {
                // 等首帧渲染完再拉服务:冷启动瞬间起 FGS 容易和进程初始化打架
                delay(1_200)
                runCatching { CrashLogger.stage(ctx, "fgs.start") }
                runCatching { LocationShareService.start(ctx) }
            } else {
                locPermLauncher.launch(LocationPermissionHelper.FOREGROUND_PERMISSIONS.toTypedArray())
            }
        }
    }

    // 当前是否在 tab 屏(login / chat / detail 全屏,没底栏)
    val showBottomBar = currentRoute == Route.HOME ||
                        currentRoute == Route.PROFILE ||
                        currentRoute?.startsWith("profile/") == true

    val pending by rootVm.pending.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        // ⚠️ 不要在这里再消费状态栏 inset:各页的 CompactTopBar 已经加了 statusBars padding,
        //    两边都加 = 顶部多出一条空白(1.0.23 及之前的问题)。底部由 bottomBar 自己处理。
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(
                    current = when {
                        currentRoute?.startsWith("profile") == true -> Route.PROFILE
                        else -> currentRoute ?: Route.HOME
                    },
                    onSelect = { route -> nav.navigateTab(route) }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            NavHost(
                navController = nav,
                startDestination = startDest,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(Route.LOGIN) {
                    LoginScreen(
                        onLoggedIn = {
                            nav.navigate(Route.HOME) {
                                popUpTo(Route.LOGIN) { inclusive = true }
                            }
                        }
                    )
                }

                // ---------- 记录(首页) ----------
                composable(Route.HOME) {
                    HomeScreen(
                        onGoProfile = { nav.navigateTab(Route.PROFILE) },
                        onOpenChat = { nav.navigate(Route.CHAT) },
                        onLoggedOut = {
                            nav.navigate(Route.LOGIN) {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        vm = homeVm
                    )
                }

                // ---------- 完整聊天窗 ----------
                composable(Route.CHAT) {
                    ChatScreen(
                        date = homeDate,
                        onBack = { nav.popBackStack() },
                        onOpenPartnerDetail = { nav.navigate(Route.PARTNER_DETAIL) },
                        vm = homeVm
                    )
                }

                // ---------- TA 的资料 ----------
                composable(Route.PARTNER_DETAIL) {
                    PartnerDetailScreen(
                        partner = homeState?.partner?.user,
                        bindAt = homeState?.me?.user?.bindAt,
                        onBack = { nav.popBackStack() }
                    )
                }

                // ---------- 我的(嵌套 Profile 图) ----------
                composable(Route.PROFILE) {
                    ProfileScreen(
                        onLoggedOut = {
                            nav.navigate(Route.LOGIN) {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        outerNav = nav
                    )
                }
            }
        }
    }

    // ===== 上次崩溃日志(便于用户反馈) =====
    var crashText by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { crashText = com.ganluo.jiyi.util.CrashLogger.readLast(ctx) }
    crashText?.let { text ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = {
                com.ganluo.jiyi.util.CrashLogger.clear(ctx); crashText = null
            },
            title = { androidx.compose.material3.Text("上次运行发生异常") },
            text = {
                androidx.compose.material3.Text(
                    text.take(3000),
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    runCatching {
                        val cm = ctx.getSystemService(android.content.Context.CLIPBOARD_SERVICE)
                                as android.content.ClipboardManager
                        cm.setPrimaryClip(android.content.ClipData.newPlainText("crash", text))
                    }
                    com.ganluo.jiyi.util.CrashLogger.clear(ctx); crashText = null
                }) { androidx.compose.material3.Text("复制并关闭") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = {
                    com.ganluo.jiyi.util.CrashLogger.clear(ctx); crashText = null
                }) { androidx.compose.material3.Text("关闭") }
            }
        )
    }
    UpdateSheet(
        pending = pending,
        onDismiss = { rootVm.dismissUpdate() },
        onDownload = { url, code -> rootVm.startDownload(url, code) },
        onInstall = { apk, expected -> rootVm.install(apk, expected) }
    )
}

/** 顶层 tab 切换 */
fun NavController.navigateTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/** Profile 嵌套 NavHost 内部用的 tab 切换(回退到 PROFILE_ROOT) */
internal fun NavHostController.profileNavigate(route: String) {
    navigate(route) {
        popUpTo(Route.PROFILE_ROOT) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}