package com.ganluo.jiyi.ui.map

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ganluo.jiyi.data.remote.dto.LocationDto
import com.ganluo.jiyi.data.repository.HomeRepository
import com.ganluo.jiyi.data.repository.LocationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 地图 + 位置共享 ViewModel(1.0.12):
 *  - 加载我和伴侣的位置(load 一次)
 *  - 30s 一次轮询(进入地图页时启动 / 离开时停止)
 *  - 后台共享 → LocationShareService(1.0.12 重构:不依赖 viewModelScope,Service 进程内跑)
 *  - 共享状态通过 [LocationSharingState] 单例同步给 UI
 */
@HiltViewModel
class MapViewModel @Inject constructor(
    private val homeRepo: HomeRepository,
    private val locRepo: LocationRepository,
    private val amap: AmapLocationProvider,
    private val sharingState: LocationSharingState,
    @ApplicationContext private val appContext: Context
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

    init {
        load()
        // 同步 Service 状态到 UI(Service 启动 / 销毁都触发)
        viewModelScope.launch {
            sharingState.sharing.collect { active ->
                _state.value = _state.value.copy(sharing = active)
            }
        }
    }

    /** 拉一次(进入页面、点刷新) */
    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, lastError = null)
            try {
                val both = locRepo.both()
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

    /** 开启后台共享 → 启动 Foreground Service */
    fun startSharing() {
        if (sharingState.sharing.value) return
        LocationShareService.start(appContext)
    }

    /** 关闭后台共享 */
    fun stopSharing() {
        LocationShareService.stop(appContext)
    }

    /** 手动"定位我":拉一次,定位成功就上传 */
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
    }
}
