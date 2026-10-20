package com.ganluo.jiyi.ui.home

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ganluo.jiyi.data.remote.ApiService
import com.ganluo.jiyi.data.remote.dto.HomeResp
import com.ganluo.jiyi.data.remote.dto.MessageDto
import com.ganluo.jiyi.data.repository.HomeRepository
import com.ganluo.jiyi.data.repository.MessageRepository
import com.ganluo.jiyi.data.repository.MoodRepository
import com.ganluo.jiyi.ui.util.DateUtil
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

/** 留言发图时的上传状态 */
sealed class ComposerUpload {
    data object Idle : ComposerUpload()
    data object Loading : ComposerUpload()
    data class Ready(val url: String, val width: Int, val height: Int) : ComposerUpload()
    data class Failed(val message: String) : ComposerUpload()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val homeRepo: HomeRepository,
    private val moodRepo: MoodRepository,
    private val msgRepo: MessageRepository,
    private val api: ApiService,
    saved: SavedStateHandle,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _date = MutableStateFlow(DateUtil.toDateStr())
    val date = _date.asStateFlow()

    private val _home = MutableStateFlow<HomeResp?>(null)
    val home = _home.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    /** 留言弹窗里图片上传状态(Idle = 没选图) */
    private val _composerUpload = MutableStateFlow<ComposerUpload>(ComposerUpload.Idle)
    val composerUpload = _composerUpload.asStateFlow()

    /** 微信式底部输入栏:输入框文字 */
    private val _composerText = MutableStateFlow("")
    val composerText = _composerText.asStateFlow()

    /** 微信式底部输入栏:引用回复的目标消息 */
    private val _replyTo = MutableStateFlow<MessageDto?>(null)
    val replyTo = _replyTo.asStateFlow()

    fun setComposerText(t: String) { _composerText.value = t }
    fun appendEmoji(emoji: String) { _composerText.value = (_composerText.value + emoji).take(500) }
    fun setReplyTo(msg: MessageDto?) { _replyTo.value = msg }
    fun clearReply() { _replyTo.value = null }
    fun clearComposer() {
        _composerText.value = ""
        _replyTo.value = null
        _composerUpload.value = ComposerUpload.Idle
    }

    fun setDate(d: String) {
        if (d == _date.value) return
        _date.value = d
        load()
        loadMonthMarks(d)
    }

    fun shift(deltaDays: Int) = setDate(DateUtil.shiftDate(_date.value, deltaDays))

    fun load() {
        _loading.value = true
        viewModelScope.launch {
            try {
                val h = homeRepo.load(_date.value)
                _home.value = h
                _error.value = null
                val unreadInDay = h.messages?.unreadInDay ?: 0
                if (unreadInDay > 0) {
                    runCatching { msgRepo.readAll(_date.value) }
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _loading.value = false
            }
        }
    }

    fun addMood(type: String, content: String?, onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                moodRepo.add(type, content, _date.value)
                load()
            } catch (_: Exception) {}
            onDone()
        }
    }

    fun deleteMood(id: Long) = viewModelScope.launch {
        runCatching { moodRepo.delete(id) }
        load()
    }

    fun sendMessage(
        content: String,
        replyToId: Long?,
        imageUrl: String? = null,
        imageWidth: Int? = null,
        imageHeight: Int? = null,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                msgRepo.send(content, replyToId, imageUrl, imageWidth, imageHeight)
                clearComposer()
                load()
            } catch (_: Exception) {}
            onDone()
        }
    }

    fun deleteMessage(id: Long) = viewModelScope.launch {
        runCatching { msgRepo.delete(id) }
        load()
    }

    /** 日历标记点:该月哪些天有心情 / 有留言(只算有内容的,不重复 fetch 整月数据太重) */
    private val _monthMarks = MutableStateFlow<Pair<Set<String>, Set<String>>>(emptySet<String>() to emptySet<String>())
    val monthMarks = _monthMarks.asStateFlow()

    /**
     * 切换日期时,顺手把当月 1..今天 的"是否有内容"扫一遍,给日历打点。
     * 后端没有月聚合端点,所以并发拉若干天(上限 31 天),失败就静默跳过,
     * 不影响主流程。日历不依赖它也能用(只是没有小圆点)。
     */
    private fun loadMonthMarks(anchorDate: String) {
        viewModelScope.launch {
            val (y, m, _) = runCatching {
                anchorDate.split("-").map { it.toInt() }
            }.getOrNull() ?: return@launch
            val cal = java.util.Calendar.getInstance().apply { set(y, m - 1, 1) }
            val daysInMonth = cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
            val today = DateUtil.toDateStr()
            val moodDays = mutableSetOf<String>()
            val msgDays = mutableSetOf<String>()
            for (d in 1..daysInMonth) {
                val ds = String.format("%04d-%02d-%02d", y, m, d)
                if (ds > today) break
                runCatching { homeRepo.load(ds) }.getOrNull()?.let { h ->
                    if (h.me?.moods?.isNotEmpty() == true || h.partner?.moods?.isNotEmpty() == true) moodDays.add(ds)
                    if (h.messages?.list?.isNotEmpty() == true) msgDays.add(ds)
                }
            }
            _monthMarks.value = moodDays to msgDays
        }
    }

    /** 图片选择入口:HomeScreen 和 ChatScreen 共用(各自持有 launcher,这里只做上传) */
    fun uploadImage(uri: Uri) = uploadMessageImage(uri)

    fun findMessage(id: Long): MessageDto? =
        _home.value?.messages?.list?.firstOrNull { it.id == id }

    /** 留言时选图:POST /api/upload/image,拿到 url + 读图片宽高,写进 composerUpload */
    fun uploadMessageImage(uri: Uri) = viewModelScope.launch {
        if (_composerUpload.value is ComposerUpload.Loading) return@launch
        _composerUpload.value = ComposerUpload.Loading
        try {
            val mime = appContext.contentResolver.getType(uri)
                ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                    uri.lastPathSegment?.substringAfterLast('.', "")
                )
                ?: "image/jpeg"
            val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime) ?: "jpg"

            val cacheFile = File(appContext.cacheDir, "msg_${System.currentTimeMillis()}.$ext")
            appContext.contentResolver.openInputStream(uri).use { input ->
                requireNotNull(input) { "无法读取图片" }
                cacheFile.outputStream().use { output -> input.copyTo(output) }
            }
            if (cacheFile.length() > 5 * 1024 * 1024) {
                cacheFile.delete()
                _composerUpload.value = ComposerUpload.Failed("图片不能超过 5MB")
                return@launch
            }

            // 读原图宽高(只读 header,内存友好)
            val (w, h) = readImageSize(cacheFile)

            val body = cacheFile.asRequestBody(mime.toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData("file", cacheFile.name, body)
            val resp = api.uploadImage(part)

            cacheFile.delete()
            _composerUpload.value = ComposerUpload.Ready(resp.url, w, h)
        } catch (e: Exception) {
            _composerUpload.value = ComposerUpload.Failed("上传失败:${e.message}")
        }
    }

    /** 取消已选图片 */
    fun clearComposerImage() {
        _composerUpload.value = ComposerUpload.Idle
    }

    private fun readImageSize(file: File): Pair<Int, Int> {
        return try {
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, opts)
            opts.outWidth.coerceAtLeast(1) to opts.outHeight.coerceAtLeast(1)
        } catch (_: Exception) {
            1080 to 1080
        }
    }
}
