package com.ganluo.jiyi.ui.home

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ganluo.jiyi.data.remote.dto.UserDto
import com.ganluo.jiyi.ui.component.Avatar
import com.ganluo.jiyi.ui.component.CompactTopBar
import com.ganluo.jiyi.ui.theme.Spacing
import com.ganluo.jiyi.ui.util.DateUtil

/**
 * TA 的资料页(从聊天窗右上角头像进):
 *  头像 / 昵称 / 签名 / 邀请码 / 绑定时间
 */
@Composable
fun PartnerDetailScreen(
    partner: UserDto?,
    bindAt: String?,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        CompactTopBar(title = "TA 的资料", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screenH, vertical = Spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Avatar(
                url = partner?.avatar,
                name = partner?.nickname,
                size = 88.dp,
                ring = true
            )
            Spacer(Modifier.height(Spacing.md))
            Text(
                text = partner?.nickname?.takeIf { it.isNotBlank() } ?: "—",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = partner?.signature?.takeIf { it.isNotBlank() }
                    ?: "TA 还没有写签名",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(Spacing.xl))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(Spacing.lg)) {
                    DetailRow("TA 的邀请码", partner?.inviteCode ?: "—")
                    Spacer(Modifier.height(Spacing.md))
                    DetailRow("绑定时间", bindAt?.takeIf { it.isNotBlank() }?.let { prettyDate(it) } ?: "—")
                }
            }

            Spacer(Modifier.height(Spacing.xl))
            Text(
                text = "你们已经绑定在一起啦",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(Spacing.xxxl))
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(80.dp)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun prettyDate(raw: String): String {
    return try {
        val s = raw.replace('T', ' ')
        val d = s.substringBefore(' ').split("-").map { it.toInt() }
        if (d.size == 3) "${d[0]}年${d[1]}月${d[2]}日" else raw
    } catch (_: Exception) {
        raw
    }
}
