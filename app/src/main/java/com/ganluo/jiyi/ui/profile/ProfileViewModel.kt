package com.ganluo.jiyi.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ganluo.jiyi.data.remote.dto.BindRequestDto
import com.ganluo.jiyi.data.remote.dto.UserDto
import com.ganluo.jiyi.data.repository.AuthRepository
import com.ganluo.jiyi.data.repository.BindRepository
import com.ganluo.jiyi.update.AppUpdateChecker
import com.ganluo.jiyi.update.UpdateCheckResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val bind: BindRepository,
    private val appUpdateChecker: AppUpdateChecker
) : ViewModel() {

    private val _me = MutableStateFlow<UserDto?>(null)
    val me = _me.asStateFlow()

    private val _incoming = MutableStateFlow<List<BindRequestDto>>(emptyList())
    val incoming = _incoming.asStateFlow()

    private val _outgoing = MutableStateFlow<List<BindRequestDto>>(emptyList())
    val outgoing = _outgoing.asStateFlow()

    private val _msg = MutableStateFlow<String?>(null)
    val msg = _msg.asStateFlow()

    // 手动检查更新的状态(null=未触发,Idle 也算 null)
    private val _updateState = MutableStateFlow<UpdateCheckResult?>(null)
    val updateState = _updateState.asStateFlow()

    private val _updateChecking = MutableStateFlow(false)
    val updateChecking = _updateChecking.asStateFlow()

    fun load() = viewModelScope.launch {
        try {
            val me = auth.me()
            _me.value = me
            val req = bind.requests()
            _incoming.value = req.incoming
            _outgoing.value = req.outgoing
        } catch (e: Exception) {
            _msg.value = e.message
        }
    }

    fun save(nickname: String, signature: String) = viewModelScope.launch {
        try {
            _me.value = auth.updateProfile(nickname, signature, null)
            _msg.value = "已保存"
        } catch (e: Exception) {
            _msg.value = e.message
        }
    }

    fun applyBind(inviteCode: String) = viewModelScope.launch {
        try {
            val res = bind.apply(inviteCode)
            _msg.value = if (res.status == "bound") "绑定成功" else "请求已发送,等 TA 同意"
            load()
        } catch (e: Exception) {
            _msg.value = e.message
        }
    }

    fun handle(requestId: Long, action: String) = viewModelScope.launch {
        try {
            bind.handle(requestId, action)
            _msg.value = if (action == "accept") "绑定成功" else "已拒绝"
            load()
        } catch (e: Exception) { _msg.value = e.message }
    }

    fun cancel(requestId: Long) = viewModelScope.launch {
        try {
            bind.cancel(requestId)
            load()
        } catch (e: Exception) { _msg.value = e.message }
    }

    fun unbind() = viewModelScope.launch {
        try {
            bind.unbind()
            _msg.value = "已解除绑定"
            load()
        } catch (e: Exception) { _msg.value = e.message }
    }

    fun logout(after: () -> Unit) = viewModelScope.launch {
        auth.logout()
        after()
    }

    /** 手动检查更新(给 Profile 里的「检查更新」按钮用) */
    fun checkUpdate() = viewModelScope.launch {
        _updateChecking.value = true
        _updateState.value = null
        _updateState.value = appUpdateChecker.tryCheck()
        _updateChecking.value = false
    }

    fun clearMsg() { _msg.value = null }
}
