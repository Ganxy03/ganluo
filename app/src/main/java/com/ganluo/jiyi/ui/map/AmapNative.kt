package com.ganluo.jiyi.ui.map

import android.util.Log

/**
 * 高德 native 库预检。
 *
 * 为什么要预检:
 *  高德 3D 地图的核心逻辑在 `libAMapSDK_MAP_v11_3_100.so` / `libapssdk.so` 里,
 *  某些设备(ABI 不匹配、系统 dlopen 限制、厂商 ROM 阉割)会加载失败,
 *  抛出 `UnsatisfiedLinkError`。这个错误是 **Error**,不是 Exception,
 *  而且 MapView 内部的 native 崩溃(C SIGSEGV)是 **Java 层完全挡不住** 的,
 *  表现就是打开地图页直接闪退。
 *
 *  所以进地图页之前先用纯 Java 的方式把 .so 加载一遍:
 *  - 成功 → 后面 SDK 自己再 loadLibrary 会直接命中已加载的句柄,安全
 *  - 失败 → 我们提前知道,直接显示降级 UI,不去碰 MapView,避免闪退
 *
 * 结果缓存,一个进程只探测一次。
 */
object AmapNative {

    private const val TAG = "AmapNative"

    private val _state: Result<Unit> by lazy {
        runCatching {
            System.loadLibrary("apssdk")
            System.loadLibrary("AMapSDK_MAP_v11_3_100")
        }.onFailure { Log.e(TAG, "amap native lib load failed", it) }
    }

    /** native 库是否可用 */
    val available: Boolean get() = _state.isSuccess

    /** 不可用时的原因(给 UI / 崩溃上报用) */
    val reason: String
        get() = _state.exceptionOrNull()?.let { "${it.javaClass.simpleName}: ${it.message}" }
            ?: "unknown"
}
