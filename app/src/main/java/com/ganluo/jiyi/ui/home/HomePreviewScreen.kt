package com.ganluo.jiyi.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.ganluo.jiyi.data.remote.dto.MessageDto
import com.ganluo.jiyi.data.remote.dto.MoodDto
import com.ganluo.jiyi.ui.component.CompactTopBar
import com.ganluo.jiyi.ui.theme.Spacing
import com.ganluo.jiyi.ui.util.DateUtil

/**
 * 首页版式预览(**仅 debug 构建**,用假数据渲染真实组件)。
 *
 * 用途:改首页排版时不用真机登录也能看效果——
 *   adb shell am start -n cn.ganluo.jiyi.debug/com.ganluo.jiyi.MainActivity --ez preview_home true
 *   adb exec-out screencap -p > home.png
 *
 * release 构建里这段代码不会被调用(入口在 MainActivity 里用 BuildConfig.DEBUG 挡住)。
 */
@Composable
fun HomePreviewScreen() {
    val today = DateUtil.toDateStr()
    var text by remember { mutableStateOf("") }
    var upload by remember { mutableStateOf<ComposerUpload>(ComposerUpload.Idle) }

    val fakeMoods = listOf(
        MoodDto(1, "happy", "开心", "今天被同事夸了,开心一整天", today, "10:24"),
        MoodDto(2, "tired", "疲惫", null, today, "21:10")
    )
    val fakeMessages = listOf(
        MessageDto(1, "今天下班早,想吃那家麻辣烫", isMine = false, isRead = true, time = "18:32"),
        MessageDto(2, "好呀,我七点到家", isMine = true, isRead = true, time = "18:35"),
        MessageDto(3, "那我先去买点水果 🍊", isMine = false, isRead = true, time = "18:40"),
        MessageDto(4, "顺便把快递取一下,有两箱", isMine = false, isRead = true, time = "18:41"),
        MessageDto(5, "收到,马上出门 🚶", isMine = true, isRead = true, time = "18:42"),
        MessageDto(6, "路上小心,外面有点风", isMine = false, isRead = true, time = "18:45")
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        // 与线上一致:外层不消费状态栏(由 CompactTopBar 负责)
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        topBar = { CompactTopBar(title = "日常") }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Spacing.screenH, vertical = Spacing.sm)
        ) {
            DatePickerCard(
                date = today,
                markedDates = setOf(today),
                moodDates = setOf(today),
                messageDates = setOf(today),
                onDateChange = {}
            )
            Spacer(Modifier.height(Spacing.sm))
            MoodCard(
                date = today,
                meNickname = "小鹿",
                meAvatar = null,
                partnerNickname = "阿柚",
                partnerAvatar = null,
                myMoods = fakeMoods,
                partnerMoods = fakeMoods.take(1),
                onAdd = {},
                onLongPressMine = {}
            )
            Spacer(Modifier.height(Spacing.sm))
            MessagePreviewCard(
                date = today,
                bound = true,
                partnerNickname = "阿柚",
                partnerAvatar = null,
                meNickname = "小鹿",
                meAvatar = null,
                messages = fakeMessages,
                onOpenChat = {},
                onGoProfile = {},
                composer = {
                    InlineMessageComposer(
                        text = text,
                        onTextChange = { text = it },
                        upload = upload,
                        onPickImage = {},
                        onClearImage = { upload = ComposerUpload.Idle },
                        onSend = { text = "" }
                    )
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}
