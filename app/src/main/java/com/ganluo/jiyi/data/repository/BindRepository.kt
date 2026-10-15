package com.ganluo.jiyi.data.repository

import com.ganluo.jiyi.data.remote.ApiService
import com.ganluo.jiyi.data.remote.dto.ApplyBindReq
import com.ganluo.jiyi.data.remote.dto.ApplyBindResp
import com.ganluo.jiyi.data.remote.dto.BindRequestsResp
import com.ganluo.jiyi.data.remote.dto.CancelBindReq
import com.ganluo.jiyi.data.remote.dto.HandleBindReq
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BindRepository @Inject constructor(private val api: ApiService) {
    suspend fun apply(inviteCode: String): ApplyBindResp = api.applyBind(ApplyBindReq(inviteCode))
    suspend fun requests(): BindRequestsResp = api.bindRequests()
    suspend fun handle(requestId: Long, action: String) =
        api.handleBind(HandleBindReq(requestId, action))
    suspend fun cancel(requestId: Long) = api.cancelBind(CancelBindReq(requestId))
    suspend fun unbind() = api.unbind()
    suspend fun search(inviteCode: String) = api.searchUser(inviteCode)
}
