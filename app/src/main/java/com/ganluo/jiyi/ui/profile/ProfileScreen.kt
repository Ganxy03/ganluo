package com.ganluo.jiyi.ui.profile

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.ganluo.jiyi.ui.theme.Card
import com.ganluo.jiyi.ui.theme.Primary
import com.ganluo.jiyi.ui.theme.PrimaryDeep
import com.ganluo.jiyi.ui.theme.PrimarySoft
import com.ganluo.jiyi.ui.theme.TextLight
import com.ganluo.jiyi.ui.theme.TextSub
import com.ganluo.jiyi.ui.util.DateUtil
import com.ganluo.jiyi.update.UpdateCheckResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onLoggedOut: () -> Unit,
    vm: ProfileViewModel = hiltViewModel()
) {
    val me by vm.me.collectAsStateWithLifecycle()
    val incoming by vm.incoming.collectAsStateWithLifecycle()
    val outgoing by vm.outgoing.collectAsStateWithLifecycle()
    val msg by vm.msg.collectAsStateWithLifecycle()
    val updateState by vm.updateState.collectAsStateWithLifecycle()
    val updateChecking by vm.updateChecking.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(Unit) { vm.load() }
    LaunchedEffect(msg) {
        msg?.let { snackbar.showSnackbar(it); vm.clearMsg() }
    }

    var nicknameInput by remember { mutableStateOf("") }
    var signatureInput by remember { mutableStateOf("") }
    var inviteInput by remember { mutableStateOf("") }
    var initOnce by remember { mutableStateOf(false) }
    LaunchedEffect(me) {
        if (me != null && !initOnce) {
            nicknameInput = me?.nickname.orEmpty()
            signatureInput = me?.signature.orEmpty()
            initOnce = true
        }
    }

    var showUnbindDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = { CompactTopBar(title = "我的") }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // 我的资料卡 — 窄屏(<360dp)改成竖排,避免两个 OutlinedTextField 被挤太窄
            JiyiCard {
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val narrow = maxWidth < 360.dp
                    if (narrow) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Avatar(url = me?.avatar, name = me?.nickname, size = 56.dp)
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = me?.nickname?.takeIf { it.isNotBlank() } ?: "未设置昵称",
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text("编辑右侧字段后保存", color = TextLight, fontSize = 11.sp)
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            OutlinedTextField(
                                value = nicknameInput,
                                onValueChange = { if (it.length <= 32) nicknameInput = it },
                                placeholder = { Text("设置昵称") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(6.dp))
                            OutlinedTextField(
                                value = signatureInput,
                                onValueChange = { if (it.length <= 100) signatureInput = it },
                                placeholder = { Text("写一句话介绍自己") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(10.dp))
                            Button(
                                onClick = { vm.save(nicknameInput, signatureInput) },
                                modifier = Modifier.fillMaxWidth().height(40.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = Color.White)
                            ) { Text("保存", fontSize = 13.sp) }
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Avatar(url = me?.avatar, name = me?.nickname, size = 64.dp)
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = nicknameInput,
                                    onValueChange = { if (it.length <= 32) nicknameInput = it },
                                    placeholder = { Text("设置昵称") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = signatureInput,
                                    onValueChange = { if (it.length <= 100) signatureInput = it },
                                    placeholder = { Text("写一句话介绍自己") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = { vm.save(nicknameInput, signatureInput) },
                                modifier = Modifier.height(36.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimarySoft, contentColor = PrimaryDeep)
                            ) { Text("保存", fontSize = 12.sp) }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // 我的邀请码
            JiyiCard {
                SectionTitle(label = "我的邀请码")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PrimarySoft, RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = me?.inviteCode ?: "——",
                        color = PrimaryDeep,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 6.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(PrimarySoft)
                            .clickable {
                                me?.inviteCode?.let { copyToClipboard(context, it) }
                            }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text("复制", color = PrimaryDeep, fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text("把这串码告诉 TA,TA 就能向你发起绑定", color = TextSub, fontSize = 11.sp)
            }

            Spacer(Modifier.height(12.dp))

            // 伴侣
            if (me?.partner != null) {
                JiyiCard {
                    SectionTitle(label = "我的另一半")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Avatar(url = me?.partner?.avatar, name = me?.partner?.nickname, size = 40.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                me?.partner?.nickname ?: "—",
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            me?.bindAt?.takeIf { it.isNotBlank() }?.let {
                                Text("绑定于 ${it.take(10)}", color = TextSub, fontSize = 11.sp)
                            }
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFFF6F2F3))
                                .clickable { showUnbindDialog = true }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text("解除", color = TextSub, fontSize = 12.sp)
                        }
                    }
                }
            } else {
                JiyiCard {
                    SectionTitle(label = "绑定另一半")
                    Text("让对方在「我的」页面把 TA 的邀请码发给你,填写后即可发起绑定请求",
                        color = TextSub, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
                    OutlinedTextField(
                        value = inviteInput,
                        onValueChange = { inviteInput = it.uppercase().take(8) },
                        placeholder = { Text("请输入对方的邀请码") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { if (inviteInput.length == 8) vm.applyBind(inviteInput) },
                        enabled = inviteInput.length == 8,
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = Color.White)
                    ) { Text("发送绑定请求") }
                }
            }

            // 收到的请求
            if (incoming.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                JiyiCard {
                    SectionTitle(label = "待处理的请求")
                    incoming.forEach { r -> IncomingRow(r, vm) }
                }
            }

            // 发出的请求
            if (outgoing.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                JiyiCard {
                    SectionTitle(label = "我发出的请求")
                    outgoing.forEach { r -> OutgoingRow(r, vm) }
                }
            }

            // 关于
            Spacer(Modifier.height(12.dp))
            JiyiCard {
                SectionTitle(label = "关于")
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val narrow = maxWidth < 360.dp
                    if (narrow) {
                        Column {
                            Text(
                                "吉壹 v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Text(BuildConfig.APPLICATION_ID, color = TextLight, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
                            Spacer(Modifier.height(10.dp))
                            Button(
                                onClick = { vm.checkUpdate() },
                                enabled = !updateChecking,
                                modifier = Modifier.fillMaxWidth().height(40.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = Color.White)
                            ) {
                                if (updateChecking) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.White
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("检查中…", fontSize = 13.sp)
                                } else {
                                    Text("检查更新", fontSize = 13.sp)
                                }
                            }
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "吉壹 v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                Text(BuildConfig.APPLICATION_ID, color = TextLight, fontSize = 11.sp)
                            }
                            Button(
                                onClick = { vm.checkUpdate() },
                                enabled = !updateChecking,
                                modifier = Modifier.height(36.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimarySoft, contentColor = PrimaryDeep)
                            ) {
                                if (updateChecking) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp,
                                        color = PrimaryDeep
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text("检查中…", fontSize = 12.sp)
                                } else {
                                    Text("检查更新", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
                updateState?.let { state ->
                    Spacer(Modifier.height(6.dp))
                    val (text, color) = when (state) {
                        is UpdateCheckResult.HasUpdate -> "✓ 发现新版本 v${state.latestName},查看弹窗升级" to PrimaryDeep
                        UpdateCheckResult.UpToDate -> "✓ 已是最新版本" to TextSub
                        is UpdateCheckResult.Failed -> "✗ 检查失败:${state.message}" to MaterialTheme.colorScheme.error
                    }
                    Text(text, color = color, fontSize = 11.sp)
                }
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { showLogoutDialog = true },
                modifier = Modifier.fillMaxWidth().height(44.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF6F2F3), contentColor = TextSub)
            ) { Text("退出登录") }
            Spacer(Modifier.height(40.dp))
        }
    }

    if (showUnbindDialog) {
        AlertDialog(
            onDismissRequest = { showUnbindDialog = false },
            title = { Text("解除绑定") },
            text = { Text("解除后将无法查看彼此的日常记录,之前的内容会保留。确定要解除吗?") },
            confirmButton = {
                TextButton(onClick = { showUnbindDialog = false; vm.unbind() }) {
                    Text("解除", color = PrimaryDeep)
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
                }) { Text("退出", color = PrimaryDeep) }
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
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(url = r.user.avatar, name = r.user.nickname, size = 32.dp)
        Spacer(Modifier.width(12.dp))
        Text(
            r.user.nickname ?: "—",
            modifier = Modifier.weight(1f),
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        TextButton(
            onClick = { vm.handle(r.requestId, "accept") },
            colors = ButtonDefaults.textButtonColors(contentColor = Color.White),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            modifier = Modifier.background(Primary, RoundedCornerShape(16.dp))
        ) { Text("同意", color = Color.White, fontSize = 12.sp) }
        Spacer(Modifier.width(8.dp))
        TextButton(
            onClick = { vm.handle(r.requestId, "reject") },
            colors = ButtonDefaults.textButtonColors(contentColor = TextSub),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            modifier = Modifier.background(Color(0xFFF6F2F3), RoundedCornerShape(16.dp))
        ) { Text("拒绝", fontSize = 12.sp) }
    }
}

@Composable
private fun OutgoingRow(r: BindRequestDto, vm: ProfileViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(url = r.user.avatar, name = r.user.nickname, size = 32.dp)
        Spacer(Modifier.width(12.dp))
        Text(
            r.user.nickname ?: "—",
            modifier = Modifier.weight(1f),
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text("等待同意", color = TextSub, fontSize = 11.sp, modifier = Modifier.padding(end = 8.dp))
        TextButton(
            onClick = { vm.cancel(r.requestId) },
            colors = ButtonDefaults.textButtonColors(contentColor = TextSub),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            modifier = Modifier.background(Color(0xFFF6F2F3), RoundedCornerShape(16.dp))
        ) { Text("撤销", fontSize = 12.sp) }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("invite_code", text))
}
