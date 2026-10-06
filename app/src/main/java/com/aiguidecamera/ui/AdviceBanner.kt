package com.aiguidecamera.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aiguidecamera.guide.Advice
import com.aiguidecamera.guide.rules.HintType

/** 프리뷰 하단 조언 배너. 조언 1개 + 방향 화살표, 바뀔 때 페이드. 대기 중(Pending)에는 아무것도 안 그린다. */
@Composable
fun AdviceBanner(advice: Advice, modifier: Modifier = Modifier) {
    AnimatedContent(
        targetState = advice,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        contentKey = { it.bannerKey() },
        label = "advice",
        modifier = modifier,
    ) { current ->
        when (current) {
            Advice.Pending -> Box(modifier = Modifier)
            Advice.Good -> BannerPill(text = "좋아요! 지금 찍어보세요", icon = Icons.Filled.CheckCircle, tint = GOOD_GREEN)
            is Advice.Problem -> BannerPill(
                text = current.issue.message,
                icon = iconFor(current.issue.hint),
                tint = Color.White,
            )
        }
    }
}

@Composable
private fun BannerPill(text: String, icon: ImageVector?, tint: Color) {
    Row(
        modifier = Modifier
            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(50))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(text = text, color = Color.White, fontSize = 15.sp)
    }
}

/** 같은 문구면 애니메이션 없이 유지한다. */
private fun Advice.bannerKey(): String = when (this) {
    Advice.Pending -> "pending"
    Advice.Good -> "good"
    is Advice.Problem -> issue.ruleId + issue.message
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

val GOOD_GREEN = Color(0xFF4CD964)
