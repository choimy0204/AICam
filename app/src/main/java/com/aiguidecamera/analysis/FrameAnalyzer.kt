package com.aiguidecamera.analysis

import android.os.SystemClock
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.aiguidecamera.guide.GuideConstants
import com.aiguidecamera.guide.ShootingMode
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executor
import kotlin.math.hypot

/**
 * CameraX 분석 프레임을 약 10fps로 받아 포즈·얼굴·밝기·센서 값을 모아 [FrameAnalysisResult] 하나로 만든다.
 * 음식 모드에서는 포즈·얼굴 검출을 건너뛴다. ImageProxy는 모든 작업이 끝난 뒤 닫는다.
 */
class FrameAnalyzer(
    private val sensorReader: SensorReader,
    /** ML Kit 완료 콜백을 받을 실행기. 카메라 분석 실행기와 같은 것을 쓴다. */
    private val callbackExecutor: Executor,
    private val onResult: (FrameAnalysisResult) -> Unit,
) : ImageAnalysis.Analyzer {

    @Volatile var mode: ShootingMode = ShootingMode.PORTRAIT

    private val poseAnalyzer = PoseAnalyzer()
    private val faceAnalyzer = FaceAnalyzer()
    private val lightAnalyzer = LightAnalyzer()

    private var lastAnalysisMs = 0L
    private var previousLandmarks: Map<BodyPart, Landmark> = emptyMap()

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(image: ImageProxy) {
        val now = SystemClock.elapsedRealtime()
        val mediaImage = image.image
        if (now - lastAnalysisMs < GuideConstants.ANALYSIS_INTERVAL_MS || mediaImage == null) {
            image.close()
            return
        }
        lastAnalysisMs = now

        val rotation = image.imageInfo.rotationDegrees
        val isSideways = rotation == 90 || rotation == 270
        val uprightWidth = if (isSideways) image.height else image.width
        val uprightHeight = if (isSideways) image.width else image.height

        if (mode == ShootingMode.FOOD) {
            finish(image, rotation, now, emptyMap(), emptyList())
            return
        }

        val input = InputImage.fromMediaImage(mediaImage, rotation)
        val poseTask = poseAnalyzer.process(input)
        val faceTask = faceAnalyzer.process(input)
        Tasks.whenAllComplete(poseTask, faceTask).addOnCompleteListener(callbackExecutor) {
            val landmarks = resultOrNull(poseTask)
                ?.let { poseAnalyzer.toLandmarks(it, uprightWidth, uprightHeight) }
                .orEmpty()
            val faces = resultOrNull(faceTask)
                ?.let { faceAnalyzer.toFaceInfos(it, uprightWidth, uprightHeight) }
                .orEmpty()
            finish(image, rotation, now, landmarks, faces)
        }
    }

    fun close() {
        poseAnalyzer.close()
        faceAnalyzer.close()
    }

    private fun finish(
        image: ImageProxy,
        rotation: Int,
        timestampMs: Long,
        landmarks: Map<BodyPart, Landmark>,
        faces: List<FaceInfo>,
    ) {
        try {
            lightAnalyzer.analyze(image, rotation, faces.firstOrNull()?.box)
        } finally {
            image.close()
        }
        val motion = poseMotion(previousLandmarks, landmarks)
        previousLandmarks = landmarks
        onResult(
            FrameAnalysisResult(
                timestampMs = timestampMs,
                landmarks = landmarks,
                faces = faces,
                faceBrightness = lightAnalyzer.faceBrightness,
                frameBrightness = lightAnalyzer.frameBrightness,
                meanRed = lightAnalyzer.meanRed,
                meanGreen = lightAnalyzer.meanGreen,
                meanBlue = lightAnalyzer.meanBlue,
                rollDeg = sensorReader.rollDeg,
                deviceRollDeg = sensorReader.deviceRollDeg,
                pitchDeg = sensorReader.pitchDeg,
                gyroMagnitude = sensorReader.consumeGyroPeak(),
                poseMotion = motion,
            ),
        )
    }

    /** 두 프레임 모두 신뢰도 기준을 넘는 관절들의 평균 이동 거리. 공통 관절이 없으면 null. */
    private fun poseMotion(previous: Map<BodyPart, Landmark>, current: Map<BodyPart, Landmark>): Float? {
        var sum = 0f
        var count = 0
        for ((part, now) in current) {
            val before = previous[part] ?: continue
            if (now.confidence < GuideConstants.LANDMARK_CONFIDENCE_MIN) continue
            if (before.confidence < GuideConstants.LANDMARK_CONFIDENCE_MIN) continue
            sum += hypot(now.x - before.x, now.y - before.y)
            count++
        }
        return if (count > 0) sum / count else null
    }

    private fun <T> resultOrNull(task: Task<T>): T? {
        if (task.isSuccessful) return task.result
        Log.w(TAG, "분석 작업 실패", task.exception)
        return null
    }

    private companion object {
        const val TAG = "FrameAnalyzer"
    }
}
