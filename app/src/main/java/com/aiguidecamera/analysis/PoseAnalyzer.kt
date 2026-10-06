package com.aiguidecamera.analysis

import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions

/**
 * ML Kit Pose Detection(스트림 모드)으로 관절 위치를 찾고, 똑바로 선 이미지 기준 0~1 좌표로 바꾼다.
 */
class PoseAnalyzer {

    private val detector = PoseDetection.getClient(
        PoseDetectorOptions.Builder()
            .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
            .build(),
    )

    fun process(image: InputImage): Task<Pose> = detector.process(image)

    /** [uprightWidth] x [uprightHeight]는 회전을 반영한(똑바로 선) 이미지 크기. */
    fun toLandmarks(pose: Pose, uprightWidth: Int, uprightHeight: Int): Map<BodyPart, Landmark> {
        val result = HashMap<BodyPart, Landmark>(LANDMARK_TYPES.size)
        for ((part, type) in LANDMARK_TYPES) {
            val landmark = pose.getPoseLandmark(type) ?: continue
            val position = landmark.position
            result[part] = Landmark(
                x = position.x / uprightWidth,
                y = position.y / uprightHeight,
                confidence = landmark.inFrameLikelihood,
            )
        }
        return result
    }

    fun close() = detector.close()

    private companion object {
        val LANDMARK_TYPES = listOf(
            BodyPart.NOSE to PoseLandmark.NOSE,
            BodyPart.LEFT_EYE to PoseLandmark.LEFT_EYE,
            BodyPart.RIGHT_EYE to PoseLandmark.RIGHT_EYE,
            BodyPart.LEFT_SHOULDER to PoseLandmark.LEFT_SHOULDER,
            BodyPart.RIGHT_SHOULDER to PoseLandmark.RIGHT_SHOULDER,
            BodyPart.LEFT_ELBOW to PoseLandmark.LEFT_ELBOW,
            BodyPart.RIGHT_ELBOW to PoseLandmark.RIGHT_ELBOW,
            BodyPart.LEFT_HIP to PoseLandmark.LEFT_HIP,
            BodyPart.RIGHT_HIP to PoseLandmark.RIGHT_HIP,
            BodyPart.LEFT_KNEE to PoseLandmark.LEFT_KNEE,
            BodyPart.RIGHT_KNEE to PoseLandmark.RIGHT_KNEE,
            BodyPart.LEFT_ANKLE to PoseLandmark.LEFT_ANKLE,
            BodyPart.RIGHT_ANKLE to PoseLandmark.RIGHT_ANKLE,
        )
    }
}
