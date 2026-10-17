package com.ganluo.jiyi.data.repository

import com.ganluo.jiyi.data.remote.ApiService
import com.ganluo.jiyi.data.remote.dto.LocationBothDto
import com.ganluo.jiyi.data.remote.dto.LocationDto
import com.ganluo.jiyi.data.remote.dto.LocationUploadReq
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 位置共享(1.0.11):
 *  - 后端每用户只存一行最新位置,UPSERT 覆盖
 *  - 前端 30s 轮询 /both,同时按需 upload
 *
 * 注意:AMap Location SDK 返回的坐标系是 GCJ-02(火星坐标),
 * 这里直接上传,后端不转换,展示时用 `CoordinateConverter` 校正。
 * (v1 简化版:不转换,后端存的也是 GCJ-02,地图显示一致)
 */
@Singleton
class LocationRepository @Inject constructor(private val api: ApiService) {

    /** 上传我的当前位置(覆盖式) */
    suspend fun upload(lat: Double, lng: Double, accuracy: Float = 0f): LocationDto =
        api.uploadLocation(LocationUploadReq(lat, lng, accuracy))

    /** 我自己的最新位置(可能 null:从未上传过) */
    suspend fun me(): LocationDto? = api.myLocation()

    /** 伴侣最新位置(null:未绑 / 伴侣未共享) */
    suspend fun partner(): LocationDto? = api.partnerLocation()

    /** 一次拿全(地图页用) */
    suspend fun both(): LocationBothDto = api.bothLocations()
}
