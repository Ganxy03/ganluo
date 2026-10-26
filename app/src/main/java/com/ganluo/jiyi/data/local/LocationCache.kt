package com.ganluo.jiyi.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ganluo.jiyi.data.remote.dto.LocationDto
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.locationDataStore by preferencesDataStore(name = "location_cache")

/**
 * 位置本地缓存(2.1):
 *
 * 目的:
 *  1. **冷启动秒显示** —— 打开地图页先渲染上次已知位置,再异步刷新,不用干等网络
 *  2. **省流量 / 省高德调用** —— 缓存命中时不必立刻重新定位
 *  3. **离线可用** —— 断网时也能看到最后一次已知位置(标注时间)
 *
 * 只存「最后一条」,不做历史。历史位置在服务端。
 */
@Singleton
class LocationCache @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val keyMeLat = floatPreferencesKey("me_lat")
    private val keyMeLng = floatPreferencesKey("me_lng")
    private val keyMeAcc = floatPreferencesKey("me_acc")
    private val keyMeTs = longPreferencesKey("me_ts")

    private val keyPaLat = floatPreferencesKey("pa_lat")
    private val keyPaLng = floatPreferencesKey("pa_lng")
    private val keyPaAcc = floatPreferencesKey("pa_acc")
    private val keyPaTs = longPreferencesKey("pa_ts")

    /** 我最后一次定位(本地 GPS 结果,不依赖网络) */
    val myLocation: Flow<LocationDto?> = context.locationDataStore.data.map { p ->
        val lat = p[keyMeLat] ?: return@map null
        val lng = p[keyMeLng] ?: return@map null
        val ts = p[keyMeTs] ?: 0L
        LocationDto(
            lat = lat.toDouble(),
            lng = lng.toDouble(),
            accuracy = p[keyMeAcc] ?: 0f,
            updatedAt = if (ts > 0) formatTs(ts) else null
        )
    }

    /** TA 最后一次已知位置(来自 /both 接口) */
    val partnerLocation: Flow<LocationDto?> = context.locationDataStore.data.map { p ->
        val lat = p[keyPaLat] ?: return@map null
        val lng = p[keyPaLng] ?: return@map null
        val ts = p[keyPaTs] ?: 0L
        LocationDto(
            lat = lat.toDouble(),
            lng = lng.toDouble(),
            accuracy = p[keyPaAcc] ?: 0f,
            updatedAt = if (ts > 0) formatTs(ts) else null
        )
    }

    suspend fun saveMy(lat: Double, lng: Double, accuracy: Float, ts: Long = System.currentTimeMillis()) {
        context.locationDataStore.edit { p ->
            p[keyMeLat] = lat.toFloat()
            p[keyMeLng] = lng.toFloat()
            p[keyMeAcc] = accuracy
            p[keyMeTs] = ts
        }
    }

    suspend fun savePartner(loc: LocationDto?) {
        context.locationDataStore.edit { p ->
            if (loc == null) {
                p.remove(keyPaLat); p.remove(keyPaLng); p.remove(keyPaAcc); p.remove(keyPaTs)
            } else {
                p[keyPaLat] = loc.lat.toFloat()
                p[keyPaLng] = loc.lng.toFloat()
                p[keyPaAcc] = loc.accuracy
                p[keyPaTs] = System.currentTimeMillis()
            }
        }
    }

    /** 同步读一次(给 Service / 快速判断用) */
    suspend fun myLocationOnce(): LocationDto? = myLocation.first()

    private fun formatTs(ts: Long): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US)
        return sdf.format(java.util.Date(ts))
    }
}