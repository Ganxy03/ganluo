package com.ganluo.jiyi.bluetooth

import kotlinx.coroutines.flow.Flow

/**
 * BLE 接口,UI 只看这个;实现藏在 BleManagerImpl,以后换库不破坏 UI。
 */
interface BleManager {
    /** 扫描结果冷流,timeout 后 cancel */
    fun scan(timeoutMs: Long = 8_000L): Flow<ScanResult>

    /** 释放 */
    fun shutdown()
}

data class ScanResult(
    val deviceAddress: String,
    val deviceName: String?,
    val rssi: Int
)
