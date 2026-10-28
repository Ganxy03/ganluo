package com.ganluo.jiyi.ui.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ganluo.jiyi.data.remote.dto.MoodDto
import com.ganluo.jiyi.ui.component.CompactTopBar
import com.ganluo.jiyi.ui.theme.Spacing

/**
 * 首页 2.0 —— 纵向三段式:
 *
 *   ┌──────────────────────────┐
 *   │  日常                     │  顶栏 44dp
 *   ├──────────────────────────┤
 *   │  ▸ ‹ 今天 ›        ⌄      │  ① 日历卡(折叠=一行 / 展开=整月)
 *   ├──────────────────────────┤
 *   │  😊 记录心情      [记录] ⌄ │  ② 心情卡(折叠=摘要 / 展开=当天历史)
 *   ├──────────────────────────┤
 *   │  💬 留言  3 条          › │  ③ 消息卡(最近 3 条 → 点进聊天窗)
 *   └──────────────────────────┘
 *
 * 三段各自独立折叠,默认全折叠,首页保持清爽。
 * 消息全文在 [ChatScreen],TA 资料在 [PartnerDetailScreen]。
 */
@Composable
fun HomeScreen(
    onGoProfile: () -> Unit,
    onLoggedOut: () -> Unit,
    onOpenChat: () -> Unit,
    vm: HomeViewModel = hiltViewModel()
) {
    val date by vm.date.collectAsStateWithLifecycle()
    val home by vm.home.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val monthMarks by vm.monthMarks.collectAsStateWithLifecycle()
    val composerText by vm.composerText.collectAsStateWithLifecycle()
    val composerUpload by vm.composerUpload.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { vm.load() }
    LaunchedEffect(error) { error?.let { snackbar.showSnackbar(it) } }

    val me = home?.me?.user
    val partner = home?.partner?.user
    val myMoods = home?.me?.moods.orEmpty()
    val partnerMoods = home?.partner?.moods.orEmpty()
    val messages = home?.messages?.list.orEmpty()

    var showMoodPicker by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<DeleteTarget?>(null) }

    // 图片选择(首页也要能发,虽然主要在聊天窗)
    val pickImage = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? -> uri?.let { vm.uploadImage(it) } }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = { CompactTopBar(title = "日常") }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screenH, vertical = Spacing.sm)
        ) {
            // ===== ① 日历 =====
            DatePickerCard(
                date = date,
                markedDates = (monthMarks.first + monthMarks.second),
                moodDates = monthMarks.first,
                messageDates = monthMarks.second,
                onDateChange = vm::setDate
            )

            Spacer(Modifier.height(Spacing.sm))

            // ===== ② 心情 =====
            MoodCard(
                date = date,
                meNickname = me?.nickname,
                meAvatar = me?.avatar,
                partnerNickname = partner?.nickname,
                partnerAvatar = partner?.avatar,
                myMoods = myMoods,
                partnerMoods = partnerMoods,
                onAdd = { showMoodPicker = true },
                onLongPressMine = { pendingDelete = DeleteTarget.Mood(it.id) }
            )

            Spacer(Modifier.height(Spacing.sm))

            // ===== ③ 留言 =====
            MessagePreviewCard(
                date = date,
                bound = partner != null,
                partnerNickname = partner?.nickname,
                partnerAvatar = partner?.avatar,
                meNickname = me?.nickname,
                meAvatar = me?.avatar,
                messages = messages,
                onOpenChat = onOpenChat,
                onGoProfile = onGoProfile,
                composer = if (partner != null) {
                    {
                        InlineMessageComposer(
                            text = composerText,
                            onTextChange = vm::setComposerText,
                            upload = composerUpload,
                            onPickImage = {
                                pickImage.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            onClearImage = vm::clearComposerImage,
                            onSend = {
                                val ready = composerUpload as? ComposerUpload.Ready
                                vm.sendMessage(
                                    content = composerText.trim(),
                                    replyToId = null,
                                    imageUrl = ready?.url,
                                    imageWidth = ready?.width,
                                    imageHeight = ready?.height
                                )
                            }
                        )
                    }
                } else null
            )

            Spacer(Modifier.height(Spacing.xxxl))
        }
    }

    // 心情选择弹窗
    MoodPickerSheet(
        visible = showMoodPicker,
        onCancel = { showMoodPicker = false },
        onConfirm = { type, content ->
            showMoodPicker = false
            vm.addMood(type, content) {}
        }
    )

    // 删除确认
    pendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(if (target is DeleteTarget.Mood) "删除这条心情" else "删除这条留言") },
            text = { Text("删除后无法恢复,确定吗?") },
            confirmButton = {
                TextButton(onClick = {
                    when (target) {
                        is DeleteTarget.Mood -> vm.deleteMood(target.id)
                        is DeleteTarget.Message -> vm.deleteMessage(target.id)
                    }
                    pendingDelete = null
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("取消") } }
        )
    }
}

private sealed class DeleteTarget {
    data class Mood(val id: Long) : DeleteTarget()
    data class Message(val id: Long) : DeleteTarget()
}