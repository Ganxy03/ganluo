package com.ganluo.jiyi.ui.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ganluo.jiyi.data.remote.dto.HomeResp
import com.ganluo.jiyi.data.remote.dto.MessageDto
import com.ganluo.jiyi.data.repository.HomeRepository
import com.ganluo.jiyi.data.repository.MessageRepository
import com.ganluo.jiyi.data.repository.MoodRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.ganluo.jiyi.ui.util.DateUtil
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val homeRepo: HomeRepository,
    private val moodRepo: MoodRepository,
    private val msgRepo: MessageRepository,
    saved: SavedStateHandle
) : ViewModel() {

    private val _date = MutableStateFlow(DateUtil.toDateStr())
    val date = _date.asStateFlow()

    private val _home = MutableStateFlow<HomeResp?>(null)
    val home = _home.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    fun setDate(d: String) {
        if (d == _date.value) return
        _date.value = d
        load()
    }

    fun shift(deltaDays: Int) = setDate(DateUtil.shiftDate(_date.value, deltaDays))

    fun load() {
        _loading.value = true
        viewModelScope.launch {
            try {
                val h = homeRepo.load(_date.value)
                _home.value = h
                _error.value = null
                // 看完当天留言就标已读
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
            } catch (_: Exception) { /* error 已在外层 toast */ }
            onDone()
        }
    }

    fun deleteMood(id: Long) = viewModelScope.launch {
        runCatching { moodRepo.delete(id) }
        load()
    }

    fun sendMessage(content: String, replyToId: Long?, onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                msgRepo.send(content, replyToId)
                load()
            } catch (_: Exception) {}
            onDone()
        }
    }

    fun deleteMessage(id: Long) = viewModelScope.launch {
        runCatching { msgRepo.delete(id) }
        load()
    }

    fun findMessage(id: Long): MessageDto? =
        _home.value?.messages?.list?.firstOrNull { it.id == id }
}
