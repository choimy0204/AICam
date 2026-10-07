package com.aiguidecamera.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aiguidecamera.guide.Advice
import com.aiguidecamera.guide.rules.HintType
import com.aiguidecamera.ui.theme.AppColors

/**
 * 프리뷰 하단 조언 배너. 조언 1개 + 방향 화살표, 바뀔 때 페이드. 대기 중(Pending)에는 아무것도 안 그린다.
 * 구도가 맞았을 때 [autoCaptureMessage]가 있으면 "좋아요" 대신 자동 촬영 대기 문구를 보여준다.
 */
@Composable
fun AdviceBanner(advice: Advice, autoCaptureMessage: String?, modifier: Modifier = Modifier) {
    val content = bannerContentFor(advice, autoCaptureMessage)
    AnimatedContent(
        targetState = content,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        contentKey = { it?.text },
        label = "advice",
        modifier = modifier,
    ) { current ->
        if (current == null) {
            Box(modifier = Modifier)
        } else {
            BannerPill(text = current.text, icon = current.icon, tint = current.tint)
        }
    }
}

private class BannerContent(val text: String, val icon: ImageVector?, val tint: Color)

private fun bannerContentFor(advice: Advice, autoCaptureMessage: String?): BannerContent? = when (advice) {
    Advice.Pending -> null
    Advice.Good -> if (autoCaptureMessage != null) {
        BannerContent(autoCaptureMessage, Icons.Filled.HourglassTop, AppColors.Good)
    } else {
        BannerContent("좋아요! 지금 찍어보세요", Icons.Filled.CheckCircle, AppColors.Good)
    }
    is Advice.Problem -> BannerContent(advice.issue.message, iconFor(advice.issue.hint), AppColors.Accent)
}

@Composable
private fun BannerPill(text: String, icon: ImageVector?, tint: Color) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .background(AppColors.Scrim, shape)
            .border(1.dp, Color.White.copy(alpha = 0.08f), shape)
            .padding(start = if (icon != null) 8.dp else 18.dp, end = 18.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            // 아이콘은 색 동그라미 안에: 초록 = 좋음, 앰버 = 고칠 점
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(tint.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
        }
        Text(text = text, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Suppress("DEPRECATION") // RotateLeft/RotateRight의 AutoMirrored 버전은 RTL에서 뒤집혀 의미가 바뀐다.
private fun iconFor(hint: HintType): ImageVector? = when (hint) {
    HintType.UP -> Icons.Filled.ArrowUpward
    HintType.DOWN -> Icons.Filled.ArrowDownward
    HintType.BACK -> Icons.Filled.ZoomOutMap
    HintType.ROTATE_LEFT -> Icons.Filled.RotateLeft
    HintType.ROTATE_RIGHT -> Icons.Filled.RotateRight
    HintType.NONE -> null
}

