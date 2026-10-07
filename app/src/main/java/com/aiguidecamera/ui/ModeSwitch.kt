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
import com.aiguidecamera.guide.FoodAngle
import com.aiguidecamera.guide.ShootingMode
import com.aiguidecamera.ui.theme.AppColors

/** 화면 상단 인물 / 음식 / 풍경 모드 토글. */
@Composable
fun ModeSwitch(
    mode: ShootingMode,
    onModeChange: (ShootingMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    SegmentedSwitch(
        items = ShootingMode.entries,
        selected = mode,
        label = ::labelOf,
        onSelect = onModeChange,
        modifier = modifier,
    )
}

/** 음식 모드 목표 각도(탑뷰 / 45°) 토글. */
@Composable
fun FoodAngleSwitch(
    angle: FoodAngle,
    onAngleChange: (FoodAngle) -> Unit,
    modifier: Modifier = Modifier,
) {
    SegmentedSwitch(
        items = FoodAngle.entries,
        selected = angle,
        label = FoodAngle::label,
        onSelect = onAngleChange,
        modifier = modifier,
        compact = true,
    )
}

@Composable
private fun <T> SegmentedSwitch(
    items: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier = modifier
            .clip(shape)
            .background(AppColors.Scrim)
            .padding(4.dp),
    ) {
        items.forEach { item ->
            val isSelected = item == selected
            Box(
                modifier = Modifier
                    .clip(shape)
                    .background(if (isSelected) AppColors.Accent else Color.Transparent)
                    .clickable { onSelect(item) }
                    .padding(horizontal = if (compact) 12.dp else 18.dp, vertical = if (compact) 4.dp else 6.dp),
            ) {
                Text(
                    text = label(item),
                    color = if (isSelected) AppColors.OnAccent else Color.White,
                    fontSize = if (compact) 12.sp else 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }
    }
}

private fun labelOf(mode: ShootingMode): String = when (mode) {
    ShootingMode.PORTRAIT -> "인물"
    ShootingMode.FOOD -> "음식"
    ShootingMode.LANDSCAPE -> "풍경"
}
