package com.aiguidecamera.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aiguidecamera.analysis.FrameAnalysisResult
import com.aiguidecamera.camera.CameraStatus
import com.aiguidecamera.guide.GuideConstants
import com.aiguidecamera.render.FrameRateMeter
import kotlinx.coroutines.flow.StateFlow

/**
 * 임계값 튜닝용 디버그 오버레이: 프리뷰 위에 포즈 랜드마크·얼굴 박스를 그리고, 기울기·밝기·프리뷰 fps·분석 지연 등 수치를 글로 보여준다.
 * 프리뷰와 분석 프레임이 모두 4:3이라 0~1 좌표를 프리뷰 영역에 그대로 대응시킨다. 전면 카메라면 좌우 반전.
 */
@Composable
fun DebugOverlay(
    analysis: StateFlow<FrameAnalysisResult?>,
    isMirrored: Boolean,
    cameraStatus: StateFlow<CameraStatus>,
    previewFrameRate: FrameRateMeter,
    modifier: Modifier = Modifier,
) {
    val result by analysis.collectAsStateWithLifecycle()
    val status by cameraStatus.collectAsStateWithLifecycle()
    Box(modifier = modifier) {
        val current = result ?: return@Box
        Canvas(modifier = Modifier.fillMaxSize()) {
            fun screenX(x: Float) = (if (isMirrored) 1f - x else x) * size.width

            for (landmark in current.landmarks.values) {
                val visible = landmark.confidence >= GuideConstants.LANDMARK_CONFIDENCE_MIN
                drawCircle(
                    color = if (visible) Color.Green else Color.Red,
                    radius = LANDMARK_RADIUS_PX,
                    center = Offset(screenX(landmark.x), landmark.y * size.height),
                )
            }
            for (face in current.faces) {
                val left = screenX(if (isMirrored) face.box.right else face.box.left)
                drawRect(
                    color = Color.Yellow,
                    topLeft = Offset(left, face.box.top * size.height),
                    size = Size(face.box.width * size.width, face.box.height * size.height),
                    style = Stroke(width = FACE_STROKE_PX),
                )
            }
        }
        Text(
            text = debugText(current, status, previewFrameRate.fps),
            color = Color.White,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            lineHeight = 13.sp,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp)
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(6.dp),
        )
    }
}

private fun debugText(result: FrameAnalysisResult, status: CameraStatus, previewFps: Float): String {
    val face = result.primaryFace
    val visibleJoints = result.landmarks.values.count { it.confidence >= GuideConstants.LANDMARK_CONFIDENCE_MIN }
    return buildString {
        appendLine("preview %.0ffps  analysis %dms".format(previewFps, result.analysisLatencyMs))
        appendLine("ev %+.2f  ext %s".format(status.exposureEv, status.extensionLabel))
        appendLine("roll %+.1f°  pitch %+.1f°  gyro %.3f".format(result.rollDeg, result.pitchDeg, result.gyroMagnitude))
        appendLine("frame Y %.2f  face Y %s".format(result.frameBrightness, result.faceBrightness?.let { "%.2f".format(it) } ?: "-"))
        appendLine(
            "RGB %.2f/%.2f/%.2f  R/B %.2f".format(
                result.meanRed, result.meanGreen, result.meanBlue,
                if (result.meanBlue > 0f) result.meanRed / result.meanBlue else 0f,
            ),
        )
        appendLine("joints %d/%d  motion %s".format(visibleJoints, result.landmarks.size, result.poseMotion?.let { "%.3f".format(it) } ?: "-"))
        appendLine("faces %d".format(result.faces.size))
        append("sky %s  clip %.2f".format(result.skyLineY?.let { "%.2f".format(it) } ?: "-", result.highlightClipRatio))
        if (face != null) {
            append(
                "  eyes %s/%s  yaw %+.0f°".format(
                    face.leftEyeOpenProbability?.let { "%.2f".format(it) } ?: "-",
                    face.rightEyeOpenProbability?.let { "%.2f".format(it) } ?: "-",
                    face.headEulerY,
                ),
            )
        }
    }
}

private const val LANDMARK_RADIUS_PX = 8f
private const val FACE_STROKE_PX = 4f
