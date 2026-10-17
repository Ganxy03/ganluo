package com.ganluo.jiyi.ui.map

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 后台位置共享状态(VM 和 Service 共享):
 *  - Service 启动时 set(true)
 *  - Service 销毁时 set(false)
 *  - MapViewModel collect 它,实时同步到 UI
 *
 * 不持久化:App 退出后,Service 跟着死,状态归零,
 * 符合"用户主动开启"的产品语义。
 */
@Singleton
class LocationSharingState @Inject constructor() {
    private val _sharing = MutableStateFlow(false)
    val sharing: StateFlow<Boolean> = _sharing.asStateFlow()
    fun set(value: Boolean) { _sharing.value = value }
}
