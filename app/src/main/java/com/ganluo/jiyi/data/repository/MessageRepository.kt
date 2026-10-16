package com.ganluo.jiyi.data.repository

import com.ganluo.jiyi.data.remote.ApiService
import com.ganluo.jiyi.data.remote.dto.MessagesBlockDto
import com.ganluo.jiyi.data.remote.dto.SendMessageReq
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageRepository @Inject constructor(private val api: ApiService) {
    suspend fun list(date: String?): MessagesBlockDto = api.messages(date)
    suspend fun send(
        content: String,
        replyToId: Long? = null,
        imageUrl: String? = null,
        imageWidth: Int? = null,
        imageHeight: Int? = null
    ) = api.sendMessage(
        SendMessageReq(
            content = content,
            replyToId = replyToId,
            imageUrl = imageUrl,
            imageWidth = imageWidth,
            imageHeight = imageHeight
        )
    )
    suspend fun readAll(date: String? = null) =
        api.readAllMessages(if (date != null) mapOf("date" to date) else emptyMap())
    suspend fun delete(id: Long) = api.deleteMessage(id)
}
