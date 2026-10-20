package com.ganluo.jiyi.ui.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ganluo.jiyi.ui.nav.Route

/**
 * 「我的」宿主:
 *  - 顶层 navRoute 由 AppNavGraph 拥有(底部 nav 的切换)
 *  - 这里内部再起一个 NavHostController 负责「我的」的子页切换
 *  - 回退走系统返回键(嵌套栈自动处理)
 */
@Composable
fun ProfileScreen(
    onLoggedOut: () -> Unit,
    outerNav: NavHostController
) {
    val innerNav = rememberNavController()

    NavHost(
        navController = innerNav,
        startDestination = Route.Profile.ROOT
    ) {
        composable(Route.Profile.ROOT) {
            ProfileMain(
                onLoggedOut = onLoggedOut,
                onOpenLocationSub = { innerNav.navigate(Route.Profile.LOCATION) },
                onOpenMapSub = { innerNav.navigate(Route.Profile.MAP) },
                onOpenBluetoothSub = { innerNav.navigate(Route.Profile.BLUETOOTH) },
                onOpenAboutSub = { innerNav.navigate(Route.Profile.ABOUT) }
            )
        }
        composable(Route.Profile.LOCATION) {
            ProfileLocationSubPage(onBack = { innerNav.popBackStack() })
        }
        composable(Route.Profile.MAP) {
            ProfileMapSubPage(onBack = { innerNav.popBackStack() })
        }
        composable(Route.Profile.BLUETOOTH) {
            ProfileBluetoothSubPage(onBack = { innerNav.popBackStack() })
        }
        composable(Route.Profile.ABOUT) {
            ProfileAboutSubPage(onBack = { innerNav.popBackStack() })
        }
    }
}
