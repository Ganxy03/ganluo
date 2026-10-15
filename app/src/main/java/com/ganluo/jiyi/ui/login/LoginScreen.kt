package com.ganluo.jiyi.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ganluo.jiyi.ui.theme.Primary
import com.ganluo.jiyi.ui.theme.PrimaryDeep
import com.ganluo.jiyi.ui.theme.TextSub

@Composable
fun LoginScreen(
    onLoggedIn: () -> Unit,
    vm: LoginViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state) {
        when (val s = state) {
            is LoginUiState.Success -> { onLoggedIn() }
            is LoginUiState.Error -> snackbar.showSnackbar(s.message)
            else -> Unit
        }
    }

    var inviteCode by remember { mutableStateOf("") }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    Brush.verticalGradient(
                        0f to Color(0xFFFFE6EB),
                        0.46f to Color(0xFFFDF6F7),
                        1f to Color(0xFFFDF6F7)
                    )
                )
                .padding(horizontal = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(160.dp))
            Text(
                text = "吉壹",
                color = PrimaryDeep,
                fontSize = 56.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 8.sp
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "只属于你们两个人的日常",
                color = Color(0xFFB58F97),
                fontSize = 14.sp,
                letterSpacing = 1.sp
            )

            Spacer(Modifier.height(80.dp))

            // 邀请码卡片
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(20.dp))
                    .padding(24.dp)
            ) {
                Text(
                    text = "用你的邀请码登录",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "首次打开 App 时,系统会随机给你 8 位邀请码。把这个码告诉 TA,TA 在「我的」页面里填入,你们就绑定了。",
                    color = TextSub,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(24.dp))
                OutlinedTextField(
                    value = inviteCode,
                    onValueChange = { inviteCode = it.uppercase().take(8) },
                    placeholder = { Text("请输入 8 位邀请码", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { vm.login(inviteCode) },
                    enabled = state !is LoginUiState.Loading,
                    colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = Color.White),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text(if (state is LoginUiState.Loading) "登录中…" else "进入吉壹")
                }
            }
        }
    }
}
