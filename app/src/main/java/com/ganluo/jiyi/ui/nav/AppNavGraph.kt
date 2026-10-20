package com.ganluo.jiyi.ui.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ganluo.jiyi.ui.home.HomeViewModel
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ganluo.jiyi.ui.home.ChatScreen
import com.ganluo.jiyi.ui.home.HomeScreen
import com.ganluo.jiyi.ui.home.PartnerDetailScreen
import com.ganluo.jiyi.ui.util.DateUtil
import com.ganluo.jiyi.ui.login.LoginScreen
import com.ganluo.jiyi.ui.login.LoginViewModel
import com.ganluo.jiyi.ui.profile.ProfileScreen
import com.ganluo.jiyi.update.UpdateSheet

/**
 * 应用根(NavHost + 底部 nav):
 *  - 顶层 3 个路由:login / home(记录) / profile(我的)
 *  - 「我的」内部嵌套一个 NavHost(Profile 自己的子图)
 *  - 没有右侧抽屉了 —— 主切换走底部 nav
 *  - 全局 UpdateSheet 放在 Scaffold 外层,所有 tab 都看得到
 *  - 有 token 时自动开启位置共享 Service(产品要求默认开启)
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

    val startDest = when (hasToken) {
        true -> Route.HOME
        false, null -> Route.LOGIN
    }

    // 有 token 时自动 start 位置共享 Service
    LaunchedEffect(hasToken) {
        if (hasToken == true) loginVm.ensureServiceStarted()
    }

    // 当前是否在 tab 屏(login 全屏,没底栏)
    val showBottomBar = currentRoute == Route.HOME ||
                        currentRoute == Route.PROFILE ||
                        currentRoute?.startsWith("profile/") == true

    val pending by rootVm.pending.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
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
                composable(Route.HOME) {
                    HomeScreen(
                        onGoProfile = { nav.navigateTab(Route.PROFILE) },
                        onOpenChat = { nav.navigate(Route.CHAT) },
                        onLoggedOut = {
                            nav.navigate(Route.LOGIN) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    )
                }
                composable(Route.CHAT) {
                    val chatVm: HomeViewModel = hiltViewModel()
                    val d by chatVm.date.collectAsStateWithLifecycle()
                    ChatScreen(
                        date = d,
                        onBack = { nav.popBackStack() },
                        onOpenPartnerDetail = { nav.navigate(Route.PARTNER_DETAIL) },
                        vm = chatVm
                    )
                }
                composable(Route.PARTNER_DETAIL) {
                    val detailVm: HomeViewModel = hiltViewModel()
                    val partner by detailVm.home.collectAsStateWithLifecycle()
                    PartnerDetailScreen(
                        partner = partner?.partner?.user,
                        bindAt = partner?.me?.user?.bindAt,
                        onBack = { nav.popBackStack() }
                    )
                }
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


