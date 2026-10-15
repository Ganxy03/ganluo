package com.ganluo.jiyi.ui.bluetooth

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ganluo.jiyi.bluetooth.BleManager
import com.ganluo.jiyi.bluetooth.ScanResult
import com.ganluo.jiyi.ui.theme.Primary
import com.ganluo.jiyi.ui.theme.PrimaryDeep
import com.ganluo.jiyi.ui.theme.PrimarySoft
import com.ganluo.jiyi.ui.theme.TextSub
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.ganluo.jiyi.permissions.PermissionRegistry
import com.ganluo.jiyi.permissions.PermissionRequester

@HiltViewModel
class BluetoothViewModel @Inject constructor(
    private val ble: BleManager
) : ViewModel() {
    private val _scanning = MutableStateFlow(false)
    val scanning = _scanning.asStateFlow()

    private val _results = mutableStateListOf<ScanResult>()
    val results: List<ScanResult> get() = _results

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    fun startScan() {
        if (_scanning.value) return
        _scanning.value = true
        _results.clear()
        viewModelScope.launch {
            try {
                ble.scan(timeoutMs = 8_000).collect { r ->
                    if (_results.none { it.deviceAddress == r.deviceAddress }) {
                        _results.add(r)
                    }
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _scanning.value = false
            }
        }
    }

    fun stopScan() { _scanning.value = false /* flow 内部会自然结束 */ }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun BluetoothScanScreen(
    onBack: () -> Unit,
    vm: BluetoothViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scanning by vm.scanning.collectAsState()
    val results = vm.results
    val error by vm.error.collectAsState()
    val perms = rememberMultiplePermissionsState(PermissionRegistry.SCAN_BLE.toList())

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("蓝牙扫描", color = Color.White, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Primary)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "扫描附近的蓝牙低功耗设备。点「开始扫描」,会按 RSSI 强度实时列出。",
                color = TextSub,
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (!perms.allPermissionsGranted) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(PermissionRequester.rationaleFor(PermissionRegistry.SCAN_BLE),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp)
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { perms.launchMultiplePermissionRequest() },
                        colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = Color.White)
                    ) { Text("授权") }
                }
            } else {
                Button(
                    onClick = { vm.startScan() },
                    enabled = !scanning,
                    colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = Color.White)
                ) { Text(if (scanning) "扫描中…" else "开始扫描") }

                error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))

                if (results.isEmpty()) {
                    Text(
                        if (scanning) "正在搜索…" else "还没有结果",
                        color = TextSub,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(24.dp)
                    )
                } else {
                    LazyColumn {
                        items(results) { r ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(PrimarySoft),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.Bluetooth, null, tint = PrimaryDeep)
                                }
                                Spacer(Modifier.padding(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(r.deviceName ?: "(未知)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Text(r.deviceAddress, color = TextSub, fontSize = 11.sp)
                                }
                                Text("${r.rssi} dBm", color = TextSub, fontSize = 12.sp)
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}
