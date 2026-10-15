package com.ganluo.jiyi.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.authDataStore by preferencesDataStore(name = "auth_store")

/**
 * 登录态 + 当前用户 JSON 缓存。
 * 注:不做加密 — 安全模型跟原小程序一样,token 是 7 天 JWT,
 * 真要加固再换成 EncryptedSharedPreferences 或 EncryptedFile。
 */
@Singleton
class TokenStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val keyToken = stringPreferencesKey("token")
    private val keyUserJson = stringPreferencesKey("user_json")

    val tokenFlow: Flow<String?> = context.authDataStore.data.map { it[keyToken] }
    val userJsonFlow: Flow<String?> = context.authDataStore.data.map { it[keyUserJson] }

    /** 同步取一次,给 OkHttp 拦截器用 */
    suspend fun tokenOnce(): String? = context.authDataStore.data.first()[keyToken]

    suspend fun setSession(token: String, userJson: String) {
        context.authDataStore.edit { prefs ->
            prefs[keyToken] = token
            prefs[keyUserJson] = userJson
        }
    }

    suspend fun clear() {
        context.authDataStore.edit { it.clear() }
    }
}
