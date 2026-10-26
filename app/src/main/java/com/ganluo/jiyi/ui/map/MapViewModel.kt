package com.ganluo.jiyi.ui.map

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ganluo.jiyi.data.local.LocationCache
import com.ganluo.jiyi.data.remote.dto.LocationDto
import com.ganluo.jiyi.data.repository.HomeRepository
import com.ganluo.jiyi.data.repository.LocationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 地图 + 位置共享 ViewModel(2.1 重写)
 *
 * 关键变化:
 *  1. **缓存优先**:进页面先用 [LocationCache] 里的上次位置秒渲染,再异步刷新
 *  2. **本地定位**:主动 requestOnce 拿自己的 GPS 坐标,不等后台 Service 上传
 *     → 用户「自己也能看到自己的定位」
 *  3. **定位状态**:idle / locating / ok / error,UI 可显示进度与人话错误
 *  4. **轮询降频**:30s → 45s(只在页面可见时),减少请求
 */
@HiltViewModel
class MapViewModel @Inject constructor(
    private val homeRepo: HomeRepository,
    private val locRepo: LocationRepository,
    private val amap: AmapLocationProvider,
    private val sharingState: LocationSharingState,
    private val cache: LocationCache,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    data class UiState(
        val loading: Boolean = true,
        /** 我的位置(优先本地 GPS,其次服务端) */
        val me: LocationDto? = null,
        /** TA 的位置 */
        val partner: LocationDto? = null,
        val partnerNickname: String? = null,
        val partnerAvatar: String? = null,
        val sharing: Boolean = false,
        /** 是否正在定位自己 */
        val locating: Boolean = false,
        /** 定位是否成功过 */
        val located: Boolean = false,
        val lastError: String? = null
    )

    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    private var pollJob: Job? = null
    private var locateJob: Job? = null

    init {
        // 1) 先把缓存渲染出来(零延迟)
        viewModelScope.launch {
            val cachedMe = cache.myLocationOnce()
            val cachedPa = runCatching { cache.partnerLocation.first() }.getOrNull()
            if (cachedMe != null || cachedPa != null) {
                _state.value = _state.value.copy(me = cachedMe ?: _state.value.me, partner = cachedPa)
            }
        }
        load()
        viewModelScope.launch {
            sharingState.sharing.collect { active ->
                _state.value = _state.value.copy(sharing = active)
            }
        }
    }

    /** 拉一次服务端数据(进入页面、点刷新) */
    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, lastError = null)
            try {
                val both = locRepo.both()
                val home = runCatching {
                    homeRepo.load(java.time.LocalDate.now().toString())
                }.getOrNull()
                _state.value = _state.value.copy(
                    loading = false,
                    // 服务端的 me 只在本地还没有 GPS 结果时兜底使用
                    me = _state.value.me ?: both.me,
                    partner = both.partner,
                    partnerNickname = home?.partner?.user?.nickname,
                    partnerAvatar = home?.partner?.user?.avatar
                )
                runCatching { cache.savePartner(both.partner) }
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, lastError = e.message)
            }
        }
    }

    /**
     * 定位自己:GPS 拿点 → 立刻显示 → 上传给后端(让 TA 也能看到)
     * 失败时把高德错误码翻译成人话写进 lastError。
     */
    fun locateMe(upload: Boolean = true) {
        if (locateJob?.isActive == true) return
        if (!LocationShareService.hasLocationPermission(appContext)) {
            _state.value = _state.value.copy(lastError = "还没有定位权限,请先允许「位置信息」")
            return
        }
        locateJob = viewModelScope.launch {
            _state.value = _state.value.copy(locating = true, lastError = null)
            try {
                var fix: AmapLocationFix? = null
                amap.requestOnce().collect { f -> fix = f }
                val f = fix
                if (f == null) {
                    _state.value = _state.value.copy(locating = false, lastError = "定位失败,请稍后重试")
                    return@launch
                }
                // 本地先显示(不管上传成功与否,自己都能看到自己)
                val local = LocationDto(
                    lat = f.lat, lng = f.lng, accuracy = f.accuracy,
                    updatedAt = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US)
                        .format(java.util.Date(f.timestamp))
                )
                _state.value = _state.value.copy(me = local, locating = false, located = true)
                runCatching { cache.saveMy(f.lat, f.lng, f.accuracy, f.timestamp) }

                if (upload) {
                    runCatching { locRepo.upload(f.lat, f.lng, f.accuracy) }
                        .onSuccess { up -> _state.value = _state.value.copy(me = up.copy(updatedAt = local.updatedAt)) }
                        .onFailure { e ->
                            _state.value = _state.value.copy(
                                lastError = "已定位,但同步给 TA 失败:${e.message}"
                            )
                        }
                }
            } catch (e: AmapLocationException) {
                _state.value = _state.value.copy(
                    locating = false,
                    lastError = AmapLocationProvider.describe(e.code, e.message)
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(locating = false, lastError = "定位失败:${e.message}")
            }
        }
    }

    /** 进入页面时自动跑一次 */
    fun autoLocateOnEnter() {
        if (_state.value.located) return
        locateMe(upload = true)
    }

    /** 45s 轮询(仅页面可见时) */
    fun startPolling() {
        if (pollJob?.isActive == true) return
        pollJob = viewModelScope.launch {
            while (true) {
                delay(POLL_MS)
                runCatching {
                    val both = locRepo.both()
                    _state.value = _state.value.copy(partner = both.partner)
                    runCatching { cache.savePartner(both.partner) }
                }
            }
        }
    }

    fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
    }

    fun startSharing() {
        LocationShareService.start(appContext)
    }

    fun stopSharing() {
        LocationShareService.stop(appContext)
    }

    fun clearError() {
        _state.value = _state.value.copy(lastError = null)
    }

    override fun onCleared() {
        super.onCleared()
        stopPolling()
    }

    companion object {
        private const val POLL_MS = 45_000L
    }
}