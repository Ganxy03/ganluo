package com.ganluo.jiyi.data.remote

import com.ganluo.jiyi.data.remote.dto.AddMealReq
import com.ganluo.jiyi.data.remote.dto.AddMoodReq
import com.ganluo.jiyi.data.remote.dto.AppVersionDto
import com.ganluo.jiyi.data.remote.dto.ApplyBindReq
import com.ganluo.jiyi.data.remote.dto.ApplyBindResp
import com.ganluo.jiyi.data.remote.dto.BindRequestsResp
import com.ganluo.jiyi.data.remote.dto.CancelBindReq
import com.ganluo.jiyi.data.remote.dto.Envelope
import com.ganluo.jiyi.data.remote.dto.HandleBindReq
import com.ganluo.jiyi.data.remote.dto.HomeResp
import com.ganluo.jiyi.data.remote.dto.LocationBothDto
import com.ganluo.jiyi.data.remote.dto.LocationDto
import com.ganluo.jiyi.data.remote.dto.LocationUploadReq
import com.ganluo.jiyi.data.remote.dto.LoginAppReq
import com.ganluo.jiyi.data.remote.dto.LoginAppResp
import com.ganluo.jiyi.data.remote.dto.MealDto
import com.ganluo.jiyi.data.remote.dto.MessageDto
import com.ganluo.jiyi.data.remote.dto.MoodDto
import com.ganluo.jiyi.data.remote.dto.SearchUserReq
import com.ganluo.jiyi.data.remote.dto.SendMessageReq
import com.ganluo.jiyi.data.remote.dto.UpdateProfileReq
import com.ganluo.jiyi.data.remote.dto.UploadResp
import com.ganluo.jiyi.data.remote.dto.UserDto
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * 与 PHP 后端 22 个端点 + 我们新增的 2 个保持 1:1。
 * 后端统一响应为 { code, message, data },此处直接接 data。
 * Retrofit 配 Moshi 自动解 Envelope 里的 data。
 */
interface ApiService {

    // ===== Auth =====
    @POST("api/auth/login-app")
    suspend fun loginApp(@Body req: LoginAppReq): LoginAppResp

    @GET("api/auth/me")
    suspend fun authMe(): UserDto

    // ===== User =====
    @GET("api/user/me")
    suspend fun userMe(): UserDto

    @PUT("api/user/me")
    suspend fun updateProfile(@Body req: UpdateProfileReq): UserDto

    @GET("api/user/search")
    suspend fun searchUser(@Query("invite_code") inviteCode: String): UserDto

    // ===== Bind =====
    @POST("api/bind/apply")
    suspend fun applyBind(@Body req: ApplyBindReq): ApplyBindResp

    @GET("api/bind/requests")
    suspend fun bindRequests(): BindRequestsResp

    @POST("api/bind/handle")
    suspend fun handleBind(@Body req: HandleBindReq): Unit

    @POST("api/bind/cancel")
    suspend fun cancelBind(@Body req: CancelBindReq): Unit

    @POST("api/bind/unbind")
    suspend fun unbind(): Unit

    // ===== Home =====
    @GET("api/home")
    suspend fun home(@Query("date") date: String? = null): HomeResp

    // ===== Moods =====
    @GET("api/moods")
    suspend fun moods(@Query("date") date: String? = null): List<MoodDto>

    @POST("api/moods")
    suspend fun addMood(@Body req: AddMoodReq): MoodDto

    @DELETE("api/moods/{id}")
    suspend fun deleteMood(@Path("id") id: Long): Unit

    // ===== Meals =====
    @GET("api/meals")
    suspend fun meals(@Query("date") date: String? = null): List<MealDto>

    @POST("api/meals")
    suspend fun addMeal(@Body req: AddMealReq): MealDto

    @DELETE("api/meals/{id}")
    suspend fun deleteMeal(@Path("id") id: Long): Unit

    // ===== Messages =====
    @GET("api/messages")
    suspend fun messages(@Query("date") date: String? = null): com.ganluo.jiyi.data.remote.dto.MessagesBlockDto

    @POST("api/messages")
    suspend fun sendMessage(@Body req: SendMessageReq): MessageDto

    @POST("api/messages/{id}/read")
    suspend fun readMessage(@Path("id") id: Long): Unit

    @POST("api/messages/read-all")
    suspend fun readAllMessages(@Body body: Map<String, String> = emptyMap()): Unit

    @DELETE("api/messages/{id}")
    suspend fun deleteMessage(@Path("id") id: Long): Unit

    // ===== Upload =====
    @Multipart
    @POST("api/upload/image")
    suspend fun uploadImage(@Part file: MultipartBody.Part): UploadResp

    // ===== Update (本项目新增端点) =====
    @GET("api/app/version")
    suspend fun appVersion(): AppVersionDto

    // ===== Location (1.0.11) =====
    @POST("api/location/upload")
    suspend fun uploadLocation(@Body req: LocationUploadReq): LocationDto

    @GET("api/location/me")
    suspend fun myLocation(): LocationDto?

    @GET("api/location/partner")
    suspend fun partnerLocation(): LocationDto?

    @GET("api/location/both")
    suspend fun bothLocations(): LocationBothDto
}
