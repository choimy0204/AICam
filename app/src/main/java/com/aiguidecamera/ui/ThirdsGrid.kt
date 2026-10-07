package com.aiguidecamera.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** 풍경 모드 프리뷰 위 3분할 격자. 수평선·피사체를 선과 교차점에 맞추는 기준이 된다. */
@Composable
fun ThirdsGrid(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = 1.dp.toPx()
        for (i in 1..2) {
            val x = size.width * i / 3f
            val y = size.height * i / 3f
            drawLine(GRID_COLOR, Offset(x, 0f), Offset(x, size.height), strokeWidth = stroke)
            drawLine(GRID_COLOR, Offset(0f, y), Offset(size.width, y), strokeWidth = stroke)
        }
    }
}

private val GRID_COLOR = Color.White.copy(alpha = 0.35f)
