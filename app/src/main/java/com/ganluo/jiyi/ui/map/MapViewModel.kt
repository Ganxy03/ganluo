package com.ganluo.jiyi.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ganluo.jiyi.data.remote.dto.LocationDto
import com.ganluo.jiyi.data.repository.HomeRepository
import com.ganluo.jiyi.data.repository.LocationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 地图 + 位置共享 ViewModel:
 *  - 加载我和伴侣的位置
 *  - 30s 一次轮询(只在地图页可见时启动 / 离开时停止)
 *  - 共享开关 → 持续定位 + 30s 一次上传
 */
@HiltViewModel
class MapViewModel @Inject constructor(
    private val homeRepo: HomeRepository,
    private val locRepo: LocationRepository,
    private val amap: AmapLocationProvider
) : ViewModel() {

    data class UiState(
        val loading: Boolean = true,
        val me: LocationDto? = null,
        val partner: LocationDto? = null,
        val partnerNickname: String? = null,
        val partnerAvatar: String? = null,
        val sharing: Boolean = false,
        val lastError: String? = null
    )

    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    private var pollJob: Job? = null
    private var shareJob: Job? = null

    init { load() }

    /** 拉一次(首页用) */
    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, lastError = null)
            try {
                val both = locRepo.both()
                // 拿伴侣昵称 + 头像(用 home 接口返回的 partner)
                val home = runCatching { homeRepo.load(java.time.LocalDate.now().toString()) }.getOrNull()
                _state.value = _state.value.copy(
                    loading = false,
                    me = both.me,
                    partner = both.partner,
                    partnerNickname = home?.partner?.user?.nickname,
                    partnerAvatar = home?.partner?.user?.avatar
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, lastError = e.message)
            }
        }
    }

    /** 启动 30s 轮询(进入地图页时调) */
    fun startPolling() {
        if (pollJob?.isActive == true) return
        pollJob = viewModelScope.launch {
            while (true) {
                try {
                    val both = locRepo.both()
                    _state.value = _state.value.copy(me = both.me, partner = both.partner)
                } catch (_: Exception) {}
                delay(30_000L)
            }
        }
    }

    /** 停止轮询(离开地图页时调) */
    fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
    }

    /** 开启共享:持续定位 + 每 30s 上传一次 */
    fun startSharing() {
        if (_state.value.sharing) return
        _state.value = _state.value.copy(sharing = true)
        shareJob = viewModelScope.launch {
            amap.startContinuous(intervalMs = 30_000L).collect { fix ->
                runCatching { locRepo.upload(fix.lat, fix.lng, fix.accuracy) }
                    .onSuccess { _state.value = _state.value.copy(me = it) }
            }
        }
    }

    /** 关闭共享 */
    fun stopSharing() {
        _state.value = _state.value.copy(sharing = false)
        shareJob?.cancel()
        shareJob = null
    }

    /** 手动"定位我"按钮:拉一次,定位成功就上传 */
    fun locateMe() {
        viewModelScope.launch {
            try {
                amap.requestOnce().collect { fix ->
                    val me = locRepo.upload(fix.lat, fix.lng, fix.accuracy)
                    _state.value = _state.value.copy(me = me)
                    return@collect
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(lastError = "定位失败:${e.message}")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopPolling()
        stopSharing()
    }
}
