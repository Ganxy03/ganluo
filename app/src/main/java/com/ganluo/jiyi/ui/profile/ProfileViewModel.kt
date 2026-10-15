package com.ganluo.jiyi.ui.profile

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ganluo.jiyi.data.remote.ApiService
import com.ganluo.jiyi.data.remote.dto.BindRequestDto
import com.ganluo.jiyi.data.remote.dto.UserDto
import com.ganluo.jiyi.data.repository.AuthRepository
import com.ganluo.jiyi.data.repository.BindRepository
import com.ganluo.jiyi.update.AppUpdateChecker
import com.ganluo.jiyi.update.UpdateCheckResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val bind: BindRepository,
    private val appUpdateChecker: AppUpdateChecker,
    private val api: ApiService,
    @ApplicationContext private val appContext: Context
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

    // 头像上传状态(null=空闲)
    private val _avatarUploading = MutableStateFlow(false)
    val avatarUploading = _avatarUploading.asStateFlow()

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

    /**
     * 头像上传:把相册/相机选的图先 POST /api/upload/image 拿 url,
     * 再 PATCH /api/auth/profile 写到 user.avatar。
     * Uri → cacheFile → MultipartBody.Part,沿用后端 field 名 "file"。
     */
    fun uploadAvatar(uri: Uri) = viewModelScope.launch {
        if (_avatarUploading.value) return@launch
        _avatarUploading.value = true
        try {
            val mime = appContext.contentResolver.getType(uri)
                ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                    uri.lastPathSegment?.substringAfterLast('.', "")
                )
                ?: "image/jpeg"
            val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime) ?: "jpg"

            // ContentResolver 读到 cacheFile(必须,因为 Uri 不能直接当 RequestBody)
            val cacheFile = File(appContext.cacheDir, "avatar_${System.currentTimeMillis()}.$ext")
            appContext.contentResolver.openInputStream(uri).use { input ->
                requireNotNull(input) { "无法读取图片" }
                cacheFile.outputStream().use { output -> input.copyTo(output) }
            }
            if (cacheFile.length() > 5 * 1024 * 1024) {
                cacheFile.delete()
                _msg.value = "图片不能超过 5MB"
                return@launch
            }

            val body = cacheFile.asRequestBody(mime.toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData("file", cacheFile.name, body)
            val resp = api.uploadImage(part)

            // 后端返回相对路径,直接 set,显示时由 Helper::toAbsoluteUrl 拼完整域名
            val updated = auth.updateProfile(_me.value?.nickname, _me.value?.signature, resp.url)
            _me.value = updated
            _msg.value = "头像已更新"
            cacheFile.delete()
        } catch (e: Exception) {
            _msg.value = "上传失败:${e.message}"
        } finally {
            _avatarUploading.value = false
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
