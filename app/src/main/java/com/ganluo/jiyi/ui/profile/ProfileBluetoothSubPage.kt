package com.ganluo.jiyi.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ganluo.jiyi.ui.bluetooth.BluetoothScanScreen
import com.ganluo.jiyi.ui.component.CompactTopBar

@Composable
fun ProfileBluetoothSubPage(
    onBack: () -> Unit
) {
    // 直接复用现成页面(它有自带的 TopAppBar,这里不嵌套 CompactTopBar 避免重复高度)
    BluetoothScanScreen(onBack = onBack)
}
