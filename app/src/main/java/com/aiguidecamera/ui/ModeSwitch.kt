package com.aiguidecamera.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aiguidecamera.guide.ShootingMode

/** 화면 상단 인물 / 음식 모드 토글. */
@Composable
fun ModeSwitch(
    mode: ShootingMode,
    onModeChange: (ShootingMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier = modifier
            .clip(shape)
            .background(Color.Black.copy(alpha = 0.45f))
            .padding(4.dp),
    ) {
        ShootingMode.entries.forEach { item ->
            val selected = item == mode
            Box(
                modifier = Modifier
                    .clip(shape)
                    .background(if (selected) Color.White else Color.Transparent)
                    .clickable { onModeChange(item) }
                    .padding(horizontal = 18.dp, vertical = 6.dp),
            ) {
                Text(
                    text = labelOf(item),
                    color = if (selected) Color.Black else Color.White,
                    fontSize = 14.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }
    }
}

private fun labelOf(mode: ShootingMode): String = when (mode) {
    ShootingMode.PORTRAIT -> "인물"
    ShootingMode.FOOD -> "음식"
}
