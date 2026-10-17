package com.ganluo.jiyi.ui.profile

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ganluo.jiyi.BuildConfig
import com.ganluo.jiyi.data.remote.dto.BindRequestDto
import com.ganluo.jiyi.ui.component.Avatar
import com.ganluo.jiyi.ui.component.CompactTopBar
import com.ganluo.jiyi.ui.component.JiyiCard
import com.ganluo.jiyi.ui.component.SectionTitle
import com.ganluo.jiyi.ui.theme.Spacing
import com.ganluo.jiyi.update.UpdateCheckResult

@Composable
fun ProfileScreen(
    onMenuClick: () -> Unit,
    onLoggedOut: () -> Unit,
    vm: ProfileViewModel = hiltViewModel()
) {
    val me by vm.me.collectAsStateWithLifecycle()
    val incoming by vm.incoming.collectAsStateWithLifecycle()
    val outgoing by vm.outgoing.collectAsStateWithLifecycle()
    val msg by vm.msg.collectAsStateWithLifecycle()
    val updateState by vm.updateState.collectAsStateWithLifecycle()
    val updateChecking by vm.updateChecking.collectAsStateWithLifecycle()
    val avatarUploading by vm.avatarUploading.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(Unit) { vm.load() }
    LaunchedEffect(msg) {
        msg?.let { snackbar.showSnackbar(it); vm.clearMsg() }
    }

    // 头像 photo picker:Android 13+ 系统弹窗,无需 READ_MEDIA_IMAGES 权限
    val pickImage = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { vm.uploadAvatar(it) }
    }
    fun launchPicker() {
        pickImage.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    var inviteInput by remember { mutableStateOf("") }
    var showEditProfile by remember { mutableStateOf(false) }
    var showUnbindDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = { CompactTopBar(title = "我的", onMenuClick = onMenuClick) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screenH, vertical = Spacing.md)
        ) {
            // ---- 个人信息卡:头像 + 名字 + 签名 + 编辑按钮 ----
            JiyiCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Avatar(
                        url = me?.avatar,
                        name = me?.nickname,
                        size = 64.dp
                    )
                    Spacer(Modifier.width(Spacing.lg))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = me?.nickname?.takeIf { it.isNotBlank() } ?: "未设置昵称",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(Spacing.xs))
                        Text(
                            text = me?.signature?.takeIf { it.isNotBlank() } ?: "点「编辑」写一句介绍自己",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.width(Spacing.sm))
                    // 编辑按钮(文字 + icon)
                    Row(
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .clickable { showEditProfile = true }
                            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(Spacing.xs))
                        Text(
                            "编辑",
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(Modifier.height(Spacing.md))

            // ---- 我的邀请码 ----
            JiyiCard {
                SectionTitle(label = "我的邀请码")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.shapes.small
                        )
                        .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = me?.inviteCode ?: "——",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable {
                                me?.inviteCode?.let { copyToClipboard(context, it) }
                            }
                            .padding(horizontal = Spacing.lg, vertical = Spacing.sm)
                    ) {
                        Text(
                            "复制",
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    "把这串码告诉 TA,TA 就能向你发起绑定",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall
                )
            }

            Spacer(Modifier.height(Spacing.md))

            // ---- 另一半 / 绑定 ----
            if (me?.partner != null) {
                JiyiCard {
                    SectionTitle(label = "我的另一半")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Avatar(
                            url = me?.partner?.avatar,
                            name = me?.partner?.nickname,
                            size = 40.dp
                        )
                        Spacer(Modifier.width(Spacing.md))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = me?.partner?.nickname ?: "—",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            me?.bindAt?.takeIf { it.isNotBlank() }?.let {
                                Text(
                                    "绑定于 ${it.take(10)}",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.medium)
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .clickable { showUnbindDialog = true }
                                .padding(horizontal = Spacing.lg, vertical = Spacing.sm)
                        ) {
                            Text(
                                "解除",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            } else {
                JiyiCard {
                    SectionTitle(label = "绑定另一半")
                    Text(
                        "让对方在「我的」页面把 TA 的邀请码发给你,填写后即可发起绑定请求",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = Spacing.sm)
                    )
                    OutlinedTextField(
                        value = inviteInput,
                        onValueChange = { inviteInput = it.uppercase().take(8) },
                        placeholder = { Text("请输入对方的邀请码") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(Spacing.md))
                    Button(
                        onClick = { if (inviteInput.length == 8) vm.applyBind(inviteInput) },
                        enabled = inviteInput.length == 8,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) { Text("发送绑定请求") }
                }
            }

            if (incoming.isNotEmpty()) {
                Spacer(Modifier.height(Spacing.md))
                JiyiCard {
                    SectionTitle(label = "待处理的请求")
                    incoming.forEach { r -> IncomingRow(r, vm) }
                }
            }

            if (outgoing.isNotEmpty()) {
                Spacer(Modifier.height(Spacing.md))
                JiyiCard {
                    SectionTitle(label = "我发出的请求")
                    outgoing.forEach { r -> OutgoingRow(r, vm) }
                }
            }

            // ---- 关于 ----
            Spacer(Modifier.height(Spacing.md))
            JiyiCard {
                SectionTitle(label = "关于")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "吉壹 v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            BuildConfig.APPLICATION_ID,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    Button(
                        onClick = { vm.checkUpdate() },
                        enabled = !updateChecking,
                        shape = MaterialTheme.shapes.medium,
                        contentPadding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.sm),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        if (updateChecking) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(Modifier.width(Spacing.sm))
                            Text("检查中…", style = MaterialTheme.typography.labelSmall)
                        } else {
                            Text("检查更新", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                updateState?.let { state ->
                    Spacer(Modifier.height(Spacing.sm))
                    val (text, color) = when (state) {
                        is UpdateCheckResult.HasUpdate -> "✓ 发现新版本 v${state.latestName},查看弹窗升级" to MaterialTheme.colorScheme.primary
                        UpdateCheckResult.UpToDate -> "✓ 已是最新版本" to MaterialTheme.colorScheme.onSurfaceVariant
                        is UpdateCheckResult.Failed -> "✗ 检查失败:${state.message}" to MaterialTheme.colorScheme.error
                    }
                    Text(
                        text,
                        color = color,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            Spacer(Modifier.height(Spacing.xxl))
            Button(
                onClick = { showLogoutDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) { Text("退出登录") }
            Spacer(Modifier.height(Spacing.xxxl))
        }
    }

    // ---- 编辑个人信息弹窗 ----
    EditProfileSheet(
        visible = showEditProfile,
        me = me,
        avatarUploading = avatarUploading,
        onDismiss = { showEditProfile = false },
        onPickAvatar = ::launchPicker,
        onSave = { nickname, signature ->
            showEditProfile = false
            vm.save(nickname, signature)
        }
    )

    if (showUnbindDialog) {
        AlertDialog(
            onDismissRequest = { showUnbindDialog = false },
            title = { Text("解除绑定") },
            text = { Text("解除后将无法查看彼此的日常记录,之前的内容会保留。确定要解除吗?") },
            confirmButton = {
                TextButton(onClick = { showUnbindDialog = false; vm.unbind() }) {
                    Text("解除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showUnbindDialog = false }) { Text("取消") } }
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("退出登录") },
            text = { Text("退出后需要重新登录,记录不会丢失。") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    vm.logout(onLoggedOut)
                }) { Text("退出", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showLogoutDialog = false }) { Text("取消") } }
        )
    }
}

@Composable
private fun IncomingRow(r: BindRequestDto, vm: ProfileViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(url = r.user.avatar, name = r.user.nickname, size = 32.dp)
        Spacer(Modifier.width(Spacing.md))
        Text(
            text = r.user.nickname ?: "—",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        TextButton(
            onClick = { vm.handle(r.requestId, "accept") },
            shape = MaterialTheme.shapes.medium,
            contentPadding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.xs),
            colors = ButtonDefaults.textButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) { Text("同意", style = MaterialTheme.typography.labelSmall) }
        Spacer(Modifier.width(Spacing.sm))
        TextButton(
            onClick = { vm.handle(r.requestId, "reject") },
            shape = MaterialTheme.shapes.medium,
            contentPadding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.xs),
            colors = ButtonDefaults.textButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) { Text("拒绝", style = MaterialTheme.typography.labelSmall) }
    }
}

@Composable
private fun OutgoingRow(r: BindRequestDto, vm: ProfileViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(url = r.user.avatar, name = r.user.nickname, size = 32.dp)
        Spacer(Modifier.width(Spacing.md))
        Text(
            text = r.user.nickname ?: "—",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            "等待同意",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(end = Spacing.sm)
        )
        TextButton(
            onClick = { vm.cancel(r.requestId) },
            shape = MaterialTheme.shapes.medium,
            contentPadding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.xs),
            colors = ButtonDefaults.textButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) { Text("撤销", style = MaterialTheme.typography.labelSmall) }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("invite_code", text))
}
