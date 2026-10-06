package com.aiguidecamera.analysis

import android.graphics.Bitmap
import android.graphics.RectF
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** 정지 사진 속 얼굴 하나. box는 사진 크기 대비 0~1, eyesOpen은 두 눈 중 덜 뜬 쪽의 확률(모르면 null). */
data class StillFace(val box: RectF, val eyesOpen: Float?)

/**
 * 정지 사진 한 장에서 얼굴을 찾는다 (피부 보정 마스크, 연사 베스트 컷의 눈 뜸 점수용).
 * 실시간 분석은 FaceAnalyzer가 따로 맡는다.
 */
class StillFaceDetector {

    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .build(),
    )

    /** 얼굴 검출 정확도엔 이 정도 크기면 충분하고, 고해상도 원본을 그대로 넣으면 느리다. */
    private val maxInputSide = 1280

    suspend fun detect(bitmap: Bitmap): List<RectF> = detectFaces(bitmap).map { it.box }

    suspend fun detectFaces(bitmap: Bitmap): List<StillFace> {
        val input = scaledForDetection(bitmap)
        try {
            val faces = suspendCancellableCoroutine { continuation ->
                detector.process(InputImage.fromBitmap(input, 0))
                    .addOnSuccessListener { continuation.resume(it) }
                    .addOnFailureListener { continuation.resumeWithException(it) }
            }
            val width = input.width.toFloat()
            val height = input.height.toFloat()
            return faces.map { face ->
                val box = face.boundingBox
                val left = face.leftEyeOpenProbability
                val right = face.rightEyeOpenProbability
                StillFace(
                    box = RectF(box.left / width, box.top / height, box.right / width, box.bottom / height),
                    eyesOpen = if (left != null && right != null) minOf(left, right) else null,
                )
            }
        } finally {
            if (input !== bitmap) input.recycle()
        }
    }

    private fun scaledForDetection(bitmap: Bitmap): Bitmap {
        val longSide = maxOf(bitmap.width, bitmap.height)
        if (longSide <= maxInputSide) return bitmap
        val scale = maxInputSide.toFloat() / longSide
        return Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
    }
}
