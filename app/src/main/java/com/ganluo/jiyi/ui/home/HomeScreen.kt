package com.ganluo.jiyi.ui.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ganluo.jiyi.data.remote.dto.MessageDto
import com.ganluo.jiyi.ui.component.CompactTopBar
import com.ganluo.jiyi.ui.component.DateBar
import com.ganluo.jiyi.ui.component.JiyiCard
import com.ganluo.jiyi.ui.theme.Spacing
import com.ganluo.jiyi.ui.util.DateUtil

@Composable
fun HomeScreen(
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
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { vm.load() }
    LaunchedEffect(error) { error?.let { snackbar.showSnackbar(it) } }

    val isToday = DateUtil.isToday(date)
    val me = home?.me?.user
    val partner = home?.partner?.user
    val myMoods = home?.me?.moods.orEmpty()
    val partnerMoods = home?.partner?.moods.orEmpty()
    val messages = home?.messages

    var showMoodPicker by remember { mutableStateOf(false) }
    var showCompose by remember { mutableStateOf(false) }
    var replyTarget by remember { mutableStateOf<MessageDto?>(null) }
    var pendingDelete by remember { mutableStateOf<DeleteTarget?>(null) }

    // 留言发图的 photo picker
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

    // 上传失败时弹 snackbar
    LaunchedEffect(composerUpload) {
        (composerUpload as? ComposerUpload.Failed)?.let {
            snackbar.showSnackbar(it.message)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = { CompactTopBar(title = "日常") }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screenH)
        ) {
            DateBar(
                date = date,
                isToday = isToday,
                onPrev = { vm.shift(-1) },
                onNext = { vm.shift(1) },
                onPick = { /* TODO: 弹 DatePickerDialog */ }
            )

            if (loading) {
                Text(
                    "加载中…",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.xxxl),
                    textAlign = TextAlign.Center
                )
            } else if (partner == null) {
                JiyiCard {
                    Text(
                        "还没有绑定另一半",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(Spacing.sm))
                    Text(
                        "去「我的」页面输入 TA 的邀请码,就能看到彼此的日常啦",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(Spacing.lg))
                    Button(
                        onClick = onGoProfile,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) { Text("去绑定") }
                }
            } else {
                MoodSection(
                    meNickname = me?.nickname,
                    meAvatar = me?.avatar,
                    partnerNickname = partner.nickname,
                    partnerAvatar = partner.avatar,
                    myMoods = myMoods,
                    partnerMoods = partnerMoods,
                    showAdd = isToday,
                    onAdd = { showMoodPicker = true },
                    onLongPressMine = { pendingDelete = DeleteTarget.Mood(it.id) }
                )
                Spacer(Modifier.height(Spacing.md))
                MessageSection(
                    bound = true,
                    messages = messages?.list.orEmpty(),
                    unreadTotal = messages?.unreadTotal ?: 0,
                    latestUnreadDate = messages?.latestUnreadDate,
                    currentDate = date,
                    onJumpUnread = { messages?.latestUnreadDate?.let { vm.setDate(it) } },
                    onCompose = { replyTarget = null; showCompose = true },
                    onTapMessage = { replyTarget = it; showCompose = true },
                    onLongPressMessage = { pendingDelete = DeleteTarget.Message(it.id) }
                )
                Spacer(Modifier.height(Spacing.bottomGap))
            }
        }
    }

    MoodPickerSheet(
        visible = showMoodPicker,
        onCancel = { showMoodPicker = false },
        onConfirm = { type, content ->
            showMoodPicker = false
            vm.addMood(type, content) {}
        }
    )

    ComposeMessageSheet(
        visible = showCompose,
        replyTo = replyTarget,
        composerUpload = composerUpload,
        onCancel = { showCompose = false; replyTarget = null },
        onPickImage = ::launchImagePicker,
        onClearImage = { vm.clearComposerImage() },
        onSend = { text, imageUrl, w, h ->
            showCompose = false
            val target = replyTarget?.id
            replyTarget = null
            vm.sendMessage(text, target, imageUrl, w, h) { }
        }
    )

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
