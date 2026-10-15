package com.ganluo.jiyi.data.remote

import com.ganluo.jiyi.data.local.TokenStore
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 自动给每个请求加 Authorization: Bearer <token>
 * 登录 / 查版本 / 上传以外的端点,401 时清掉 token(由 AppRoot 收到事件跳登录)。
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenStore: TokenStore
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val path = original.url.encodedPath

        // 登录 / 版本检测 / 预检 OPTIONS 都不需要 token
        val skip = path.contains("/auth/login") ||
                   path.contains("/app/version") ||
                   original.method == "OPTIONS"

        val builder = original.newBuilder()
            .addHeader("Accept", "application/json")
        if (!skip) {
            val token = runBlocking { tokenStore.tokenOnce() }
            if (!token.isNullOrBlank()) {
                builder.addHeader("Authorization", "Bearer $token")
            }
        }
        return chain.proceed(builder.build())
    }
}
