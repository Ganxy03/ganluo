package com.ganluo.jiyi.data.repository

import com.ganluo.jiyi.data.local.TokenStore
import com.ganluo.jiyi.data.remote.ApiService
import com.ganluo.jiyi.data.remote.dto.LoginAppReq
import com.ganluo.jiyi.data.remote.dto.UpdateProfileReq
import com.ganluo.jiyi.data.remote.dto.UserDto
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val api: ApiService,
    private val tokenStore: TokenStore
) {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val userAdapter = moshi.adapter(UserDto::class.java)

    /** 邀请码登录,失败抛 ApiException */
    suspend fun login(inviteCode: String): UserDto {
        val resp = api.loginApp(LoginAppReq(inviteCode.trim().uppercase()))
        tokenStore.setSession(resp.token, userAdapter.toJson(resp.user))
        return resp.user
    }

    suspend fun me(): UserDto {
        val me = api.authMe()
        tokenStore.setSession(
            tokenStore.tokenOnce().orEmpty(),
            userAdapter.toJson(me)
        )
        return me
    }

    suspend fun updateProfile(nickname: String?, signature: String?, avatar: String?): UserDto {
        val updated = api.updateProfile(UpdateProfileReq(nickname, signature, avatar))
        tokenStore.setSession(tokenStore.tokenOnce().orEmpty(), userAdapter.toJson(updated))
        return updated
    }

    suspend fun logout() = tokenStore.clear()
}
