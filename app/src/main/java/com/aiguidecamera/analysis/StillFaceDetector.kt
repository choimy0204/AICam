package com.aiguidecamera.analysis

import android.graphics.Bitmap
import android.graphics.RectF
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * 정지 사진 한 장에서 얼굴 박스를 찾는다 (촬영 저장·사후 편집의 피부 보정 마스크용).
 * 결과는 사진 크기 대비 0~1 좌표다. 실시간 분석은 Phase 3의 FaceAnalyzer가 따로 맡는다.
 */
class StillFaceDetector {

    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .build(),
    )

    /** 얼굴 검출 정확도엔 이 정도 크기면 충분하고, 고해상도 원본을 그대로 넣으면 느리다. */
    private val maxInputSide = 1280

    suspend fun detect(bitmap: Bitmap): List<RectF> {
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
                RectF(box.left / width, box.top / height, box.right / width, box.bottom / height)
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
