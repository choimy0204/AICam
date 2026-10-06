package com.aiguidecamera.capture

import com.aiguidecamera.analysis.FaceInfo
import com.aiguidecamera.guide.Advice
import com.aiguidecamera.guide.ShootingMode
import com.aiguidecamera.guide.TestFrames.face
import com.aiguidecamera.guide.TestFrames.frame
import com.aiguidecamera.guide.rules.HintType
import com.aiguidecamera.guide.rules.Issue
import org.junit.Assert.assertEquals
import org.junit.Test

class CaptureReadinessTest {

    private val openFace = face(top = 0.1f, bottom = 0.3f)

    private fun portrait(
        advice: Advice = Advice.Good,
        faces: List<FaceInfo> = listOf(openFace),
        motion: Float? = 0.002f,
        gyro: Float = 0.01f,
    ) = CaptureReadiness.evaluate(advice, frame(faces = faces, motion = motion, gyro = gyro), ShootingMode.PORTRAIT)

    @Test
    fun `모든 조건 충족이면 준비 완료`() {
        assertEquals(Readiness.READY, portrait())
    }

    @Test
    fun `구도가 안 맞으면 준비 안 됨`() {
        val problem = Advice.Problem(Issue("horizon", 5, "기울었어요", HintType.ROTATE_LEFT))
        assertEquals(Readiness.COMPOSITION_NOT_READY, portrait(advice = problem))
        assertEquals(Readiness.COMPOSITION_NOT_READY, portrait(advice = Advice.Pending))
    }

    @Test
    fun `카메라가 흔들리면 기다린다`() {
        assertEquals(Readiness.CAMERA_SHAKING, portrait(gyro = 0.2f))
    }

    @Test
    fun `얼굴이 없으면 기다린다`() {
        assertEquals(Readiness.NO_FACE, portrait(faces = emptyList()))
    }

    @Test
    fun `한쪽 눈이라도 감으면 기다린다`() {
        assertEquals(Readiness.EYES_CLOSED, portrait(faces = listOf(face(0.1f, 0.3f, leftEye = 0.2f))))
        assertEquals(Readiness.EYES_CLOSED, portrait(faces = listOf(face(0.1f, 0.3f, rightEye = 0.7f))))
    }

    @Test
    fun `눈 뜸 확률을 모르면 뜬 것으로 보지 않는다`() {
        assertEquals(Readiness.EYES_CLOSED, portrait(faces = listOf(face(0.1f, 0.3f, leftEye = null))))
    }

    @Test
    fun `고개를 돌리면 기다린다`() {
        assertEquals(Readiness.FACE_TURNED, portrait(faces = listOf(face(0.1f, 0.3f, yaw = -20f))))
    }

    @Test
    fun `피사체가 움직이거나 움직임을 모르면 기다린다`() {
        assertEquals(Readiness.SUBJECT_MOVING, portrait(motion = 0.05f))
        assertEquals(Readiness.SUBJECT_MOVING, portrait(motion = null))
    }

    @Test
    fun `음식 모드는 얼굴 조건을 보지 않는다`() {
        val result = frame(gyro = 0.01f)
        assertEquals(Readiness.READY, CaptureReadiness.evaluate(Advice.Good, result, ShootingMode.FOOD))
    }

    @Test
    fun `음식 모드도 흔들리면 기다린다`() {
        val result = frame(gyro = 0.1f)
        assertEquals(Readiness.CAMERA_SHAKING, CaptureReadiness.evaluate(Advice.Good, result, ShootingMode.FOOD))
    }
}
