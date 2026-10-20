package com.ganluo.jiyi.ui.login

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ganluo.jiyi.data.local.TokenStore
import com.ganluo.jiyi.data.repository.AuthRepository
import com.ganluo.jiyi.ui.map.LocationShareService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val auth: AuthRepository,
    tokenStore: TokenStore,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    /** token 是否存在,null = 正在读 */
    val hasToken = tokenStore.tokenFlow
        .map { !it.isNullOrBlank() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _state = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val state = _state.asStateFlow()

    fun login(inviteCode: String) {
        if (inviteCode.length != 8) {
            _state.value = LoginUiState.Error("邀请码必须是 8 位")
            return
        }
        _state.value = LoginUiState.Loading
        viewModelScope.launch {
            try {
                val user = auth.login(inviteCode)
                // 登录成功 → 默认开启位置共享(产品要求)
                LocationShareService.start(appContext)
                _state.value = LoginUiState.Success(user)
            } catch (e: Exception) {
                _state.value = LoginUiState.Error(e.message ?: "登录失败")
            }
        }
    }

    /**
     * 已经在本地有 token 时(冷启动)也想自动开 Service。UI 层在确认 hasToken=true 时调一下。
     */
    fun ensureServiceStarted() {
        LocationShareService.start(appContext)
    }

    fun reset() { _state.value = LoginUiState.Idle }
}

sealed interface LoginUiState {
    data object Idle : LoginUiState
    data object Loading : LoginUiState
    data class Success(val user: com.ganluo.jiyi.data.remote.dto.UserDto) : LoginUiState
    data class Error(val message: String) : LoginUiState
}
