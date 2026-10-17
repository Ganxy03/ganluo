package com.ganluo.jiyi.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * 后端统一响应包络:
 *   { "code": 0, "message": "ok", "data": ... }
 * code = 0 成功,其余失败,message 是人话。
 */
@JsonClass(generateAdapter = true)
data class Envelope<T>(
    val code: Int,
    val message: String? = null,
    val data: T? = null
)

// ===== Auth =====

@JsonClass(generateAdapter = true)
data class LoginAppReq(
    @Json(name = "invite_code") val inviteCode: String
)

@JsonClass(generateAdapter = true)
data class LoginAppResp(
    val token: String,
    val user: UserDto
)

@JsonClass(generateAdapter = true)
data class UserDto(
    val id: Long,
    val nickname: String?,
    val avatar: String?,
    @Json(name = "invite_code") val inviteCode: String?,
    val signature: String?,
    val bound: Boolean = false,
    val partner: UserDto? = null,
    @Json(name = "bind_at") val bindAt: String? = null
)

// ===== User =====

@JsonClass(generateAdapter = true)
data class UpdateProfileReq(
    val nickname: String? = null,
    val signature: String? = null,
    val avatar: String? = null
)

@JsonClass(generateAdapter = true)
data class SearchUserReq(
    @Json(name = "invite_code") val inviteCode: String
)

// ===== Bind =====

@JsonClass(generateAdapter = true)
data class ApplyBindReq(
    @Json(name = "invite_code") val inviteCode: String
)

@JsonClass(generateAdapter = true)
data class ApplyBindResp(
    val status: String  // "sent" 或 "bound"
)

@JsonClass(generateAdapter = true)
data class HandleBindReq(
    @Json(name = "request_id") val requestId: Long,
    val action: String  // "accept" 或 "reject"
)

@JsonClass(generateAdapter = true)
data class CancelBindReq(
    @Json(name = "request_id") val requestId: Long
)

@JsonClass(generateAdapter = true)
data class BindRequestsResp(
    val incoming: List<BindRequestDto> = emptyList(),
    val outgoing: List<BindRequestDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class BindRequestDto(
    @Json(name = "request_id") val requestId: Long,
    val user: UserDto
)

// ===== Home aggregate =====

@JsonClass(generateAdapter = true)
data class HomeResp(
    val me: PersonBlockDto,
    val partner: PersonBlockDto? = null,
    val messages: MessagesBlockDto? = null
)

@JsonClass(generateAdapter = true)
data class PersonBlockDto(
    val user: UserDto,
    val moods: List<MoodDto> = emptyList(),
    val meals: List<MealDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class MessagesBlockDto(
    val date: String? = null,
    val partner: UserDto? = null,
    val list: List<MessageDto> = emptyList(),
    @Json(name = "unread_in_day") val unreadInDay: Int = 0,
    @Json(name = "unread_total") val unreadTotal: Int = 0,
    @Json(name = "latest_unread_date") val latestUnreadDate: String? = null
)

// ===== Moods =====

@JsonClass(generateAdapter = true)
data class MoodDto(
    val id: Long,
    @Json(name = "mood_type") val moodType: String,
    @Json(name = "mood_label") val moodLabel: String,
    val content: String?,
    @Json(name = "created_at") val createdAt: String,
    val time: String? = null  // 后端有时直接给 HH:mm
)

@JsonClass(generateAdapter = true)
data class AddMoodReq(
    @Json(name = "mood_type") val moodType: String,
    val content: String? = null,
    @Json(name = "mood_date") val moodDate: String
)

// ===== Meals =====

@JsonClass(generateAdapter = true)
data class MealDto(
    val id: Long,
    @Json(name = "meal_type") val mealType: String,
    val content: String?,
    val images: List<String> = emptyList(),
    @Json(name = "meal_date") val mealDate: String
)

@JsonClass(generateAdapter = true)
data class AddMealReq(
    @Json(name = "meal_type") val mealType: String,
    val content: String? = null,
    val images: List<String> = emptyList(),
    @Json(name = "meal_date") val mealDate: String
)

// ===== Messages =====

@JsonClass(generateAdapter = true)
data class MessageDto(
    val id: Long,
    val content: String,
    @Json(name = "image_url") val imageUrl: String? = null,
    @Json(name = "image_width") val imageWidth: Int? = null,
    @Json(name = "image_height") val imageHeight: Int? = null,
    @Json(name = "is_mine") val isMine: Boolean = false,
    @Json(name = "is_read") val isRead: Boolean = true,
    val time: String? = null,
    @Json(name = "reply_to") val replyTo: MessageDto? = null
)

@JsonClass(generateAdapter = true)
data class SendMessageReq(
    val content: String,
    @Json(name = "reply_to_id") val replyToId: Long? = null,
    @Json(name = "image_url") val imageUrl: String? = null,
    @Json(name = "image_width") val imageWidth: Int? = null,
    @Json(name = "image_height") val imageHeight: Int? = null
)

// ===== Upload =====

@JsonClass(generateAdapter = true)
data class UploadResp(
    val url: String
)

// ===== Update (新增端点) =====

@JsonClass(generateAdapter = true)
data class AppVersionDto(
    @Json(name = "latest_code") val latestCode: Int,
    @Json(name = "latest_name") val latestName: String,
    @Json(name = "download_url") val downloadUrl: String,
    val sha256: String? = null,
    val changelog: String? = null,
    @Json(name = "force_update") val forceUpdate: Boolean = false,
    @Json(name = "min_supported") val minSupported: Int = 0
)

// ===== Location =====

/**
 * 单点位置(me / partner 共用)
 *  - lat / lng: WGS-84,Android 端拿到 GCJ-02 时由调用方转换
 *  - updated_at: 后端 datetime 字串,客户端自己解析
 */
@JsonClass(generateAdapter = true)
data class LocationDto(
    val lat: Double,
    val lng: Double,
    val accuracy: Float = 0f,
    @Json(name = "updated_at") val updatedAt: String? = null
)

/**
 * /api/location/both 的响应:{ me: LocationDto?, partner: LocationDto? }
 *  - 上传时用不到,只在地图页一次性拉
 */
@JsonClass(generateAdapter = true)
data class LocationBothDto(
    val me: LocationDto? = null,
    val partner: LocationDto? = null
)

/** 上传位置请求体 */
@JsonClass(generateAdapter = true)
data class LocationUploadReq(
    val lat: Double,
    val lng: Double,
    val accuracy: Float = 0f
)
