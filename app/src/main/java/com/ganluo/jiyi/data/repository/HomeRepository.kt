package com.ganluo.jiyi.data.repository

import com.ganluo.jiyi.data.remote.ApiService
import com.ganluo.jiyi.data.remote.dto.HomeResp
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HomeRepository @Inject constructor(private val api: ApiService) {
    suspend fun load(date: String?): HomeResp = api.home(date)
}
