package com.ganluo.jiyi.data.repository

import com.ganluo.jiyi.data.remote.ApiService
import com.ganluo.jiyi.data.remote.dto.AddMoodReq
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MoodRepository @Inject constructor(private val api: ApiService) {
    suspend fun add(moodType: String, content: String?, date: String) =
        api.addMood(AddMoodReq(moodType, content?.takeIf { it.isNotBlank() }, date))

    suspend fun delete(id: Long) = api.deleteMood(id)
}
