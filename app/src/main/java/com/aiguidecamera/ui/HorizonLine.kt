package com.aiguidecamera.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aiguidecamera.analysis.FrameAnalysisResult
import com.aiguidecamera.guide.GuideConstants
import com.aiguidecamera.ui.theme.AppColors
import kotlin.math.abs
import kotlinx.coroutines.flow.StateFlow

/**
 * 화면 중앙 수평선. 실제 수평 방향으로 기울여 그리고, 허용 오차 안이면 초록으로 바뀐다.
 * 롤 값이 10fps로 바뀌므로 여기서만 수집해 다른 화면 요소가 다시 그려지지 않게 한다.
 */
@Composable
fun HorizonLine(analysis: StateFlow<FrameAnalysisResult?>, modifier: Modifier = Modifier) {
    val result by analysis.collectAsStateWithLifecycle()
    val current = result ?: return
    val level = abs(current.rollDeg) <= GuideConstants.HORIZON_TOLERANCE_DEG
    Canvas(modifier = modifier) {
        val halfLength = size.width * LINE_WIDTH_FRACTION / 2f
        // 폰이 시계 방향으로 돌면(롤 음수) 실제 수평은 화면에서 반시계로 보인다 → 롤만큼 회전.
        // 가로로 잡아도 맞도록 정규화 전 롤을 쓴다.
        rotate(degrees = current.deviceRollDeg) {
            drawLine(
                color = if (level) AppColors.Good else Color.White,
                start = Offset(center.x - halfLength, center.y),
                end = Offset(center.x + halfLength, center.y),
                strokeWidth = 2.dp.toPx(),
            )
        }
    }
}

private const val LINE_WIDTH_FRACTION = 0.6f
