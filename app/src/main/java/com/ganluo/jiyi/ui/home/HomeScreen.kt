package com.ganluo.jiyi.ui.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ganluo.jiyi.data.remote.dto.MessageDto
import com.ganluo.jiyi.ui.component.CompactTopBar
import com.ganluo.jiyi.ui.theme.Spacing
import com.ganluo.jiyi.ui.util.DateUtil

/**
 * 首页(方案3:微信式聊天优先)
 *
 * 布局:
 *   Scaffold(topBar = "日常")
 *   ├─ MoodStrip(顶,整条可点 → MoodSheet)
 *   ├─ MessageSection(占满剩余,reverseLayout 消息列表)
 *   └─ MessageComposer(底,常驻,微信式:文字/表情/图片/引用)
 *
 * 没有 DateBar(日期切换搬进了 MoodSheet 顶栏)
 * 没有"写留言"按钮(发送走 MessageComposer 的 ➤ 按钮)
 * 没有 AlertDialog 写留言(改成常驻底部栏)
 */
@Composable
fun HomeScreen(
    onMenuClick: () -> Unit,
    onGoProfile: () -> Unit,
    onOpenBluetooth: () -> Unit,
    onLoggedOut: () -> Unit,
    vm: HomeViewModel = hiltViewModel()
) {
    val date by vm.date.collectAsStateWithLifecycle()
    val home by vm.home.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val composerUpload by vm.composerUpload.collectAsStateWithLifecycle()
    val composerText by vm.composerText.collectAsStateWithLifecycle()
    val replyTo by vm.replyTo.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { vm.load() }
    LaunchedEffect(error) { error?.let { snackbar.showSnackbar(it) } }

    val isToday = DateUtil.isToday(date)
    val me = home?.me?.user
    val partner = home?.partner?.user
    val myMoods = home?.me?.moods.orEmpty()
    val partnerMoods = home?.partner?.moods.orEmpty()
    val messages = home?.messages

    var showMoodSheet by remember { mutableStateOf(false) }
    var showMoodPicker by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<DeleteTarget?>(null) }

    // 留言配图 photo picker
    val pickImage = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { vm.uploadMessageImage(it) }
    }
    fun launchImagePicker() {
        pickImage.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    LaunchedEffect(composerUpload) {
        (composerUpload as? ComposerUpload.Failed)?.let {
            snackbar.showSnackbar(it.message)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = { CompactTopBar(title = "日常", onMenuClick = onMenuClick) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ===== 心情 strip =====
            MoodStrip(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.screenH, vertical = Spacing.sm),
                meNickname = me?.nickname,
                meAvatar = me?.avatar,
                partnerNickname = partner?.nickname,
                partnerAvatar = partner?.avatar,
                myLatest = myMoods.maxByOrNull { it.id },
                partnerLatest = partnerMoods.maxByOrNull { it.id },
                onOpen = { showMoodSheet = true }
            )

            // ===== 消息列表(占满剩余) =====
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = true)
                    .padding(horizontal = Spacing.screenH)
            ) {
                when {
                    loading -> CenteredText("加载中…")
                    partner == null -> NotBoundCard(onGoProfile = onGoProfile)
                    else -> MessageSection(
                        modifier = Modifier.fillMaxSize(),
                        bound = true,
                        messages = messages?.list.orEmpty(),
                        unreadTotal = messages?.unreadTotal ?: 0,
                        latestUnreadDate = messages?.latestUnreadDate,
                        currentDate = date,
                        onJumpUnread = { messages?.latestUnreadDate?.let { vm.setDate(it) } },
                        // 短按消息 = 直接进引用回复模式(微信)
                        onTapMessage = { vm.setReplyTo(it) },
                        onLongPressMessage = { pendingDelete = DeleteTarget.Message(it.id) }
                    )
                }
            }

            // ===== 底部输入栏(只有绑定了伴侣才显示) =====
            if (partner != null) {
                MessageComposer(
                    text = composerText,
                    onTextChange = vm::setComposerText,
                    replyTo = replyTo,
                    onClearReply = vm::clearReply,
                    composerUpload = composerUpload,
                    onPickImage = ::launchImagePicker,
                    onClearImage = vm::clearComposerImage,
                    onEmojiPick = vm::appendEmoji,
                    onSend = {
                        val ready = composerUpload as? ComposerUpload.Ready
                        vm.sendMessage(
                            content = composerText.trim(),
                            replyToId = replyTo?.id,
                            imageUrl = ready?.url,
                            imageWidth = ready?.width,
                            imageHeight = ready?.height
                        )
                    }
                )
            }
        }
    }

    // ===== 心情 sheet =====
    MoodSheet(
        visible = showMoodSheet,
        date = date,
        isToday = isToday,
        meNickname = me?.nickname,
        meAvatar = me?.avatar,
        partnerNickname = partner?.nickname,
        partnerAvatar = partner?.avatar,
        myMoods = myMoods,
        partnerMoods = partnerMoods,
        onDismiss = { showMoodSheet = false },
        onPrev = { vm.shift(-1) },
        onNext = { vm.shift(1) },
        onAdd = { showMoodSheet = false; showMoodPicker = true },
        onLongPressMine = { pendingDelete = DeleteTarget.Mood(it.id) }
    )

    // 心情选择(从 sheet 内部触发,弹回选择 emoji + 文字)
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

@Composable
private fun CenteredText(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun NotBoundCard(onGoProfile: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "还没有绑定另一半",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(Spacing.sm))
            Text(
                "去「我的」页面输入 TA 的邀请码,就能看到彼此的日常啦",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(Spacing.lg))
            Button(
                onClick = onGoProfile,
                modifier = Modifier.fillMaxWidth().height(44.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) { Text("去绑定") }
        }
    }
}

private sealed class DeleteTarget {
    data class Mood(val id: Long) : DeleteTarget()
    data class Message(val id: Long) : DeleteTarget()
}
