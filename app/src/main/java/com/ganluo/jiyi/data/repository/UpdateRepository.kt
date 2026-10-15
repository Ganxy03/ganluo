package com.ganluo.jiyi.data.repository

import com.ganluo.jiyi.data.remote.ApiService
import com.ganluo.jiyi.data.remote.dto.AppVersionDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateRepository @Inject constructor(private val api: ApiService) {
    suspend fun latest(): AppVersionDto = api.appVersion()
}
