package com.aiguidecamera.analysis

import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions

/**
 * ML Kit Face Detection(분류 옵션: 눈 뜸 확률)으로 실시간 얼굴을 찾는다.
 * 결과 박스는 똑바로 선 이미지 기준 0~1 좌표, 큰 얼굴부터 정렬한다.
 */
class FaceAnalyzer {

    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .build(),
    )

    fun process(image: InputImage): Task<List<Face>> = detector.process(image)

    fun toFaceInfos(faces: List<Face>, uprightWidth: Int, uprightHeight: Int): List<FaceInfo> {
        val width = uprightWidth.toFloat()
        val height = uprightHeight.toFloat()
        return faces
            .sortedByDescending { it.boundingBox.width() * it.boundingBox.height() }
            .map { face ->
                val box = face.boundingBox
                FaceInfo(
                    box = NormRect(
                        (box.left / width).coerceIn(0f, 1f),
                        (box.top / height).coerceIn(0f, 1f),
                        (box.right / width).coerceIn(0f, 1f),
                        (box.bottom / height).coerceIn(0f, 1f),
                    ),
                    leftEyeOpenProbability = face.leftEyeOpenProbability,
                    rightEyeOpenProbability = face.rightEyeOpenProbability,
                    headEulerY = face.headEulerAngleY,
                )
            }
    }

    fun close() = detector.close()
}
