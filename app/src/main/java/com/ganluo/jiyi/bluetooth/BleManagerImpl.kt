package com.ganluo.jiyi.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult as AndroidScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 用 Android 原生 BluetoothLeScanner,通过 callbackFlow 包成冷流。
 * 不引第三方 BLE 库,适合协议简单场景(1-2 service)。
 */
@Singleton
class BleManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : BleManager {

    private val adapter: BluetoothAdapter? by lazy {
        val mgr = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        mgr?.adapter
    }

    private fun hasScanPerm(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(
                context, Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
        }
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    override fun scan(timeoutMs: Long): Flow<ScanResult> = callbackFlow {
        if (!hasScanPerm()) {
            close(IllegalStateException("缺少蓝牙扫描权限"))
            return@callbackFlow
        }
        val a = adapter
        if (a == null || !a.isEnabled) {
            close(IllegalStateException("请先打开蓝牙"))
            return@callbackFlow
        }
        val scanner = a.bluetoothLeScanner ?: run {
            close(IllegalStateException("设备不支持 BLE 扫描"))
            return@callbackFlow
        }

        val cb = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: AndroidScanResult) {
                val dev = result.device ?: return
                trySend(
                    ScanResult(
                        deviceAddress = dev.address,
                        deviceName = result.scanRecord?.deviceName ?: dev.name,
                        rssi = result.rssi
                    )
                )
            }
        }
        scanner.startScan(cb)

        // 用本流自己的 scope 起 timeout,awaitClose 时 cancel 掉
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val ticker: Job = scope.launch {
            delay(timeoutMs)
            if (isActive) {
                try { scanner.stopScan(cb) } catch (_: Throwable) {}
            }
        }
        awaitClose {
            try { scanner.stopScan(cb) } catch (_: Throwable) {}
            ticker.cancel()
            scope.cancel()
        }
    }

    override fun shutdown() { /* 暂不维护长连接 */ }
}
