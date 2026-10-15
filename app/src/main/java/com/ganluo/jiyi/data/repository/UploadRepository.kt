package com.ganluo.jiyi.data.repository

import com.ganluo.jiyi.data.remote.ApiService
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UploadRepository @Inject constructor(private val api: ApiService) {
    suspend fun uploadImage(file: File): String {
        val body = file.asRequestBody("image/*".toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("file", file.name, body)
        return api.uploadImage(part).url
    }
}
