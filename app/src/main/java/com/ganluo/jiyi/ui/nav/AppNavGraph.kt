package com.ganluo.jiyi.ui.nav

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
import com.ganluo.jiyi.ui.map.MapScreen
import com.ganluo.jiyi.ui.profile.ProfileScreen
import com.ganluo.jiyi.ui.theme.Spacing
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
        null -> Route.LOGIN
    }

    // 抽屉开关状态
    var drawerOpen by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
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
                        onMenuClick = { drawerOpen = true },
                        onGoProfile = { nav.navigate(Route.PROFILE) },
                        onOpenBluetooth = { nav.navigate(Route.BLUETOOTH) },
                        onLoggedOut = {
                            nav.navigate(Route.LOGIN) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    )
                }
                composable(Route.MAP) {
                    MapScreen(onMenuClick = { drawerOpen = true })
                }
                composable(Route.PROFILE) {
                    ProfileScreen(
                        onMenuClick = { drawerOpen = true },
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
    }

    // 全局更新弹窗:叠在 NavHost 之上,登录/首页/我的都能看到
    UpdateSheet(
        pending = pending,
        onDismiss = { rootVm.dismissUpdate() },
        onDownload = { url, code -> rootVm.startDownload(url, code) },
        onInstall = { apk, expected -> rootVm.install(apk, expected) }
    )

    // 右侧滑出抽屉(主 tab 切换)
    RightSideDrawer(
        visible = drawerOpen,
        onDismiss = { drawerOpen = false }
    ) {
        DrawerHeader()
        Spacer(Modifier.height(Spacing.md))
        DrawerNavItem(
            icon = Icons.Filled.Home,
            label = "日常",
            selected = current == Route.HOME,
            onClick = {
                drawerOpen = false
                nav.navigateTab(Route.HOME)
            }
        )
        DrawerNavItem(
            icon = Icons.Filled.LocationOn,
            label = "地图",
            selected = current == Route.MAP,
            onClick = {
                drawerOpen = false
                nav.navigateTab(Route.MAP)
            }
        )
        DrawerNavItem(
            icon = Icons.Filled.Person,
            label = "我的",
            selected = current == Route.PROFILE,
            onClick = {
                drawerOpen = false
                nav.navigateTab(Route.PROFILE)
            }
        )
    }
}

@Composable
private fun DrawerHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = Spacing.lg, vertical = Spacing.xl)
    ) {
        Text(
            "吉壹",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            "两个人的日常",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun DrawerNavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val containerColor =
        if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surface
    val onColor =
        if (selected) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = Spacing.xs)
            .clip(MaterialTheme.shapes.medium)
            .background(containerColor)
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.md, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(
                    if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceContainerHigh
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint =
                    if (selected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.width(Spacing.md))
        Text(
            label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = onColor,
            modifier = Modifier.weight(1f)
        )
        if (selected) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = onColor,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

private fun NavController.navigateTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
