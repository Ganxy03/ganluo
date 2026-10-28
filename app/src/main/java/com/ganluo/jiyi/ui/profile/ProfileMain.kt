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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.SystemUpdate
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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ganluo.jiyi.BuildConfig
import com.ganluo.jiyi.data.remote.dto.BindRequestDto
import com.ganluo.jiyi.ui.component.Avatar
import com.ganluo.jiyi.ui.FeatureFlags
import com.ganluo.jiyi.ui.component.CompactTopBar
import com.ganluo.jiyi.ui.component.SectionTitle
import com.ganluo.jiyi.ui.theme.Primary
import com.ganluo.jiyi.ui.theme.PrimaryDeep
import com.ganluo.jiyi.ui.theme.Spacing
import com.ganluo.jiyi.update.UpdateCheckResult

/**
 * 「我的」主页(2.0):
 *  - 头部渐变 Header(头像 + 昵称 + 签名 + 编辑)
 *  - 我的邀请码
 *  - 我的另一半 / 绑定
 *  - 待处理的请求 / 我发出的请求
 *  - 分组列表:
 *      更多: 位置共享 / 查看地图 / 蓝牙设备
 *      系统: 检查更新 / 关于 / 退出登录
 *  - 默认开启位置共享(状态展示态,subpage 内可关)
 */
@Composable
fun ProfileMain(
    onLoggedOut: () -> Unit,
    onOpenLocationSub: () -> Unit,
    onOpenMapSub: () -> Unit,
    onOpenBluetoothSub: () -> Unit,
    onOpenAboutSub: () -> Unit,
    vm: ProfileViewModel = hiltViewModel()
) {
    val me by vm.me.collectAsStateWithLifecycle()
    val incoming by vm.incoming.collectAsStateWithLifecycle()
    val outgoing by vm.outgoing.collectAsStateWithLifecycle()
    val msg by vm.msg.collectAsStateWithLifecycle()
    val updateState by vm.updateState.collectAsStateWithLifecycle()
    val updateChecking by vm.updateChecking.collectAsStateWithLifecycle()
    val avatarUploading by vm.avatarUploading.collectAsStateWithLifecycle()
    val sharing by vm.sharing.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { vm.load() }
    LaunchedEffect(msg) { msg?.let { snackbar.showSnackbar(it); vm.clearMsg() } }

    val context = LocalContext.current
    val pickImage = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? -> uri?.let { vm.uploadAvatar(it) } }
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
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screenH, vertical = Spacing.md)
        ) {
            // ===== 头部个人卡 =====
            ProfileHeader(
                nickname = me?.nickname,
                avatar = me?.avatar,
                signature = me?.signature,
                avatarUploading = avatarUploading,
                onEdit = { showEditProfile = true }
            )
            Spacer(Modifier.height(Spacing.lg))

            // ===== 我的邀请码 =====
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(Spacing.lg)) {
                    SectionTitle(label = "我的邀请码")
                    Spacer(Modifier.height(Spacing.sm))
                    val code = me?.inviteCode
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(horizontal = Spacing.md, vertical = Spacing.md),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = code ?: "——",
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Box(
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.medium)
                                .background(Primary)
                                .clickable(enabled = code != null) { code?.let { copyToClipboard(context, it) } }
                                .padding(horizontal = Spacing.lg, vertical = Spacing.sm)
                        ) {
                            Text(
                                "复制",
                                color = MaterialTheme.colorScheme.onPrimary,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        "把这串码告诉 TA,TA 就能向你发起绑定",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(Spacing.md))

            // ===== 我的另一半 / 绑定 =====
            if (me?.partner != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(modifier = Modifier.padding(Spacing.lg)) {
                        SectionTitle(label = "我的另一半")
                        Spacer(Modifier.height(Spacing.sm))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Avatar(url = me?.partner?.avatar, name = me?.partner?.nickname, size = 44.dp)
                            Spacer(Modifier.width(Spacing.md))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = me?.partner?.nickname ?: "—",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                me?.bindAt?.takeIf { it.isNotBlank() }?.let {
                                    Text(
                                        "绑定于 ${it.take(10)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                }
            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(modifier = Modifier.padding(Spacing.lg)) {
                        SectionTitle(label = "绑定另一半")
                        Text(
                            "让对方在「我的」把 TA 的邀请码发给你,填写后即可发起绑定请求",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = Spacing.sm)
                        )
                        OutlinedTextField(
                            value = inviteInput,
                            onValueChange = { inviteInput = it.uppercase().take(8) },
                            placeholder = { Text("请输入对方的邀请码") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(Spacing.sm))
                        Button(
                            onClick = { if (inviteInput.length == 8) vm.applyBind(inviteInput) },
                            enabled = inviteInput.length == 8,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            shape = MaterialTheme.shapes.medium,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) { Text("发送绑定请求", fontWeight = FontWeight.SemiBold) }
                    }
                }
            }

            // 待处理 / 我发出 的请求(合并卡片)
            if (incoming.isNotEmpty() || outgoing.isNotEmpty()) {
                Spacer(Modifier.height(Spacing.md))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(modifier = Modifier.padding(Spacing.lg)) {
                        if (incoming.isNotEmpty()) {
                            SectionTitle(label = "待处理的请求")
                            Spacer(Modifier.height(Spacing.xs))
                            incoming.forEach { r ->
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
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    TextButton(
                                        onClick = { vm.handle(r.requestId, "accept") },
                                        shape = MaterialTheme.shapes.medium,
                                        contentPadding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.xs),
                                        colors = ButtonDefaults.textButtonColors(
                                            containerColor = Primary,
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
                        }
                        if (outgoing.isNotEmpty()) {
                            SectionTitle(label = "我发出的请求")
                            Spacer(Modifier.height(Spacing.xs))
                            outgoing.forEach { r ->
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
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        "等待同意",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                    Spacer(Modifier.width(Spacing.sm))
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
                        }
                    }
                }
            }

            Spacer(Modifier.height(Spacing.lg))

            // ===== 功能列表(iOS 风格) =====
            SectionTitle(label = "更多")
            Spacer(Modifier.height(Spacing.xs))
            GroupedList {
                // 地图 / 定位共享:1.0.24 起暂时隐藏(机型兼容性问题),见 FeatureFlags.LOCATION
                if (FeatureFlags.LOCATION) {
                    ListRow(
                        icon = Icons.Filled.LocationOn,
                        title = "位置共享",
                        subtitle = if (sharing) "已开启 · TA 在地图能看到你"
                                  else "未运行",
                        subtitleColor = if (sharing) Primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        onClick = onOpenLocationSub
                    )
                    GroupedDivider()
                    ListRow(
                        icon = Icons.Filled.Map,
                        title = "查看地图",
                        subtitle = "查看我和 TA 的位置",
                        onClick = onOpenMapSub
                    )
                    GroupedDivider()
                }
                ListRow(
                    icon = Icons.Filled.Bluetooth,
                    title = "蓝牙设备",
                    subtitle = "扫描并配对低功耗设备",
                    onClick = onOpenBluetoothSub
                )
            }

            Spacer(Modifier.height(Spacing.lg))

            SectionTitle(label = "系统")
            Spacer(Modifier.height(Spacing.xs))
            GroupedList {
                ListRow(
                    icon = Icons.Filled.SystemUpdate,
                    title = "检查更新",
                    subtitle = when (val s = updateState) {
                        is UpdateCheckResult.HasUpdate -> "有新版本 v${s.latestName}"
                        UpdateCheckResult.UpToDate -> "已是最新版本"
                        is UpdateCheckResult.Failed -> "检查失败"
                        null -> "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
                    },
                    trailing = if (updateChecking) {{
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }} else null,
                    onClick = { vm.checkUpdate() }
                )
                GroupedDivider()
                ListRow(
                    icon = Icons.Filled.Info,
                    title = "关于吉壹",
                    onClick = onOpenAboutSub
                )
                GroupedDivider()
                ListRow(
                    icon = Icons.Filled.Logout,
                    title = "退出登录",
                    titleColor = MaterialTheme.colorScheme.error,
                    iconTint = MaterialTheme.colorScheme.error,
                    onClick = { showLogoutDialog = true }
                )
            }
            Spacer(Modifier.height(Spacing.xxxl))
        }
    }

    EditProfileSheet(
        visible = showEditProfile,
        me = me,
        avatarUploading = avatarUploading,
        onDismiss = { showEditProfile = false },
        onPickAvatar = { launchPicker() },
        onSave = { n, s ->
            showEditProfile = false
            vm.save(n, s)
        }
    )

    if (showUnbindDialog) {
        AlertDialog(
            onDismissRequest = { showUnbindDialog = false },
            title = { Text("解除绑定") },
            text = { Text("解除后将无法查看彼此的日常记录,之前的内容会保留。确定要解除吗?") },
            confirmButton = {
                TextButton(onClick = {
                    showUnbindDialog = false; vm.unbind()
                }) { Text("解除", color = MaterialTheme.colorScheme.error) }
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
                    showLogoutDialog = false; vm.logout(onLoggedOut)
                }) { Text("退出", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showLogoutDialog = false }) { Text("取消") } }
        )
    }
}

// ---------- 子组件 ----------

@Composable
private fun ProfileHeader(
    nickname: String?,
    avatar: String?,
    signature: String?,
    avatarUploading: Boolean,
    onEdit: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(72.dp)) {
                    Avatar(
                        url = avatar,
                        name = nickname ?: "?",
                        size = 72.dp,
                        ring = true
                    )
                    if (avatarUploading) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.45f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
                Spacer(Modifier.width(Spacing.md))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = nickname?.takeIf { it.isNotBlank() } ?: "未设置昵称",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        text = signature?.takeIf { it.isNotBlank() } ?: "点击右侧编辑,写下你对 TA 的话",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.medium)
                        .background(Primary)
                        .clickable(onClick = onEdit)
                        .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(Spacing.xs))
                        Text(
                            "编辑",
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

// ====== iOS 风格分组列表 ======

@Composable
private fun GroupedList(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface
    ) {
        androidx.compose.foundation.layout.Column(modifier = Modifier.fillMaxWidth(), content = content)
    }
}

@Composable
private fun GroupedDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 56.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    )
}

@Composable
private fun ListRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    subtitleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    iconTint: Color = PrimaryDeep,
    trailing: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.width(Spacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = titleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = subtitleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (trailing != null) {
            trailing()
            Spacer(Modifier.width(Spacing.sm))
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("invite_code", text))
}

