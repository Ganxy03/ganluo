package com.ganluo.jiyi.ui.home

import androidx.compose.foundation.background
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ganluo.jiyi.data.remote.dto.MessageDto
import com.ganluo.jiyi.ui.component.Avatar
import com.ganluo.jiyi.ui.component.MessageBubble
import com.ganluo.jiyi.ui.theme.Spacing
import com.ganluo.jiyi.ui.util.DateUtil

/**
 * 完整聊天窗(2.0):
 *  - 顶栏: ‹ 返回 | 聊天对象昵称 | 右上角 头像 → 点进 TA 的详情页
 *  - 中部: 当天全部消息(旧在上新在下,自动滚到底)
 *  - 底部: 常驻输入栏(表情 / 图片 / 引用回复 / 发送)
 *
 * 数据复用 [HomeViewModel](同一天),所以首页 → 聊天 → 返回 首页,状态都在。
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ChatScreen(
    date: String,
    onBack: () -> Unit,
    onOpenPartnerDetail: () -> Unit,
    vm: HomeViewModel = hiltViewModel()
) {
    val home by vm.home.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()
    val composerUpload by vm.composerUpload.collectAsStateWithLifecycle()
    val composerText by vm.composerText.collectAsStateWithLifecycle()
    val replyTo by vm.replyTo.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    val me = home?.me?.user
    val partner = home?.partner?.user
    val messages = home?.messages?.list.orEmpty()

    var pendingDelete by remember { mutableStateOf<MessageDto?>(null) }

    // 图片选择
    val pickLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? -> uri?.let { vm.uploadImage(it) } }

    val listState = rememberLazyListState()
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            ChatTopBar(
                title = partner?.nickname ?: "TA",
                subtitle = DateUtil.friendlyDate(date),
                avatar = partner?.avatar,
                onBack = onBack,
                onOpenDetail = { if (partner != null) onOpenPartnerDetail() }
            )
        },
        bottomBar = {
            if (partner != null) {
                MessageComposer(
                    text = composerText,
                    onTextChange = vm::setComposerText,
                    replyTo = replyTo,
                    onClearReply = vm::clearReply,
                    composerUpload = composerUpload,
                    onPickImage = {
                        pickLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
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
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (partner == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "还没有绑定另一半",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = Spacing.screenH)
                ) {
                    // 顶部日期气泡
                    item(key = "date_header") {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.sm),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = DateUtil.friendlyDate(date),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .background(
                                        MaterialTheme.colorScheme.surfaceContainer,
                                        MaterialTheme.shapes.small
                                    )
                                    .padding(horizontal = Spacing.sm, vertical = 3.dp)
                            )
                        }
                    }

                    if (loading && messages.isEmpty()) {
                        item(key = "loading") {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(Spacing.xl),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "加载中…",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else if (messages.isEmpty()) {
                        item(key = "empty") {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(Spacing.xl),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "这天还没有留言,说点什么吧",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        items(messages.size, key = { messages[it].id }) { idx ->
                            val m = messages[idx]
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = { vm.setReplyTo(m) },
                                        onLongClick = { pendingDelete = m }
                                    )
                            ) {
                                MessageBubble(msg = m, onClick = {}, onLongClick = {})
                            }
                        }
                    }
                }
            }
        }
    }

    if (pendingDelete != null) {
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除这条留言") },
            text = { Text("删除后无法恢复,确定吗?") },
            confirmButton = {
                TextButton(onClick = {
                    pendingDelete?.let { vm.deleteMessage(it.id) }
                    pendingDelete = null
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("取消") }
            }
        )
    }
}

/** 聊天窗顶栏:返回 + 昵称 + 右上角头像(点进详情) */
@Composable
private fun ChatTopBar(
    title: String,
    subtitle: String,
    avatar: String?,
    onBack: () -> Unit,
    onOpenDetail: () -> Unit
) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Column(modifier = Modifier.weight(1f).padding(horizontal = Spacing.xs)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                // 右上角头像 → 详情
                Row(
                    modifier = Modifier
                        .clickable(onClick = onOpenDetail)
                        .padding(horizontal = Spacing.sm, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Avatar(url = avatar, name = title, size = 32.dp)
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "TA 的资料",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            )
        }
    }
}


