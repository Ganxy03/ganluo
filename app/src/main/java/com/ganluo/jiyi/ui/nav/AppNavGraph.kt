package com.ganluo.jiyi.ui.nav

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ganluo.jiyi.ui.bluetooth.BluetoothScanScreen
import com.ganluo.jiyi.ui.home.HomeScreen
import com.ganluo.jiyi.ui.login.LoginScreen
import com.ganluo.jiyi.ui.login.LoginViewModel
import com.ganluo.jiyi.ui.profile.ProfileScreen
import com.ganluo.jiyi.ui.theme.Primary
import com.ganluo.jiyi.ui.theme.TextSub
import com.ganluo.jiyi.update.UpdateSheet

@Composable
fun AppRoot() {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination?.route

    // 是否已登录由 LoginViewModel 的 tokenFlow 决定
    val loginVm: LoginViewModel = hiltViewModel()
    val hasToken by loginVm.hasToken.collectAsState(initial = null)

    // 顶层 AppRootViewModel:挂更新检查状态,提供下载/安装入口
    val rootVm: AppRootViewModel = hiltViewModel()
    val pending by rootVm.pending.collectAsState()

    // 启动判断:有 token → Home,没 token → Login
    val startDest = when (hasToken) {
        true -> Route.HOME
        false -> Route.LOGIN
        null -> Route.LOGIN  // 短暂 loading,直接进登录页,有 token 跳走
    }

    Scaffold(
        bottomBar = {
            if (current == Route.HOME || current == Route.PROFILE) {
                NavigationBar(containerColor = androidx.compose.ui.graphics.Color.White) {
                    NavigationBarItem(
                        selected = current == Route.HOME,
                        onClick = { nav.navigateTab(Route.HOME) },
                        icon = { Icon(Icons.Filled.Home, contentDescription = "日常") },
                        label = { Text("日常") },
                        colors = androidx.compose.material3.NavigationBarItemDefaults.colors(
                            selectedIconColor = Primary,
                            selectedTextColor = Primary,
                            unselectedIconColor = TextSub,
                            unselectedTextColor = TextSub,
                            indicatorColor = androidx.compose.ui.graphics.Color.Transparent
                        )
                    )
                    NavigationBarItem(
                        selected = current == Route.PROFILE,
                        onClick = { nav.navigateTab(Route.PROFILE) },
                        icon = { Icon(Icons.Filled.Person, contentDescription = "我的") },
                        label = { Text("我的") },
                        colors = androidx.compose.material3.NavigationBarItemDefaults.colors(
                            selectedIconColor = Primary,
                            selectedTextColor = Primary,
                            unselectedIconColor = TextSub,
                            unselectedTextColor = TextSub,
                            indicatorColor = androidx.compose.ui.graphics.Color.Transparent
                        )
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = startDest,
            modifier = Modifier.fillMaxSize().padding(padding)
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
                    onGoProfile = { nav.navigate(Route.PROFILE) },
                    onOpenBluetooth = { nav.navigate(Route.BLUETOOTH) },
                    onLoggedOut = {
                        nav.navigate(Route.LOGIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
            composable(Route.PROFILE) {
                ProfileScreen(
                    onLoggedOut = {
                        nav.navigate(Route.LOGIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
            composable(Route.BLUETOOTH) {
                BluetoothScanScreen(onBack = { nav.popBackStack() })
            }
        }
    }

    // 全局更新弹窗:叠在 NavHost 之上,登录/首页/我的都能看到
    UpdateSheet(
        pending = pending,
        onDismiss = { rootVm.dismissUpdate() },
        onDownload = { url, code -> rootVm.startDownload(url, code) },
        onInstall = { apk, expected -> rootVm.install(apk, expected) }
    )
}

private fun NavController.navigateTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
