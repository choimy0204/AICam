package com.aiguidecamera.camera

import com.aiguidecamera.analysis.NormRect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraAssistTest {

    private val face = NormRect(0.4f, 0.2f, 0.6f, 0.4f)
    private val movedFace = NormRect(0.6f, 0.2f, 0.8f, 0.4f)

    @Test
    fun `얼굴이 보이면 측정을 걸고 가만히 있으면 다시 걸지 않는다`() {
        val policy = FaceMeteringPolicy()
        assertEquals(FaceMeteringPolicy.Action.METER, policy.update(face, 10_000))
        assertEquals(FaceMeteringPolicy.Action.NONE, policy.update(face, 11_000))
    }

    @Test
    fun `얼굴이 크게 움직이면 다시 건다`() {
        val policy = FaceMeteringPolicy()
        policy.update(face, 10_000)
        assertEquals(FaceMeteringPolicy.Action.METER, policy.update(movedFace, 11_000))
    }

    @Test
    fun `얼굴이 잠깐 안 보이면 유지하고 오래 안 보이면 푼다`() {
        val policy = FaceMeteringPolicy()
        policy.update(face, 10_000)
        assertEquals(FaceMeteringPolicy.Action.NONE, policy.update(null, 10_500))
        assertEquals(FaceMeteringPolicy.Action.CANCEL, policy.update(null, 11_500))
        assertEquals(FaceMeteringPolicy.Action.NONE, policy.update(null, 12_000))
    }

    @Test
    fun `하늘이 날아가면 노출을 내리고 자리 잡을 때까지 기다린다`() {
        val tuner = AutoExposureTuner()
        assertTrue(tuner.update(0.3f, 10_000))
        assertEquals(-1f / 3f, tuner.ev, 1e-4f)
        assertFalse(tuner.update(0.3f, 10_300))
    }

    @Test
    fun `날아간 곳이 없으면 0까지만 올린다`() {
        val tuner = AutoExposureTuner()
        tuner.update(0.3f, 10_000)
        assertTrue(tuner.update(0f, 11_000))
        assertEquals(0f, tuner.ev, 1e-4f)
        assertFalse(tuner.update(0f, 12_000))
    }

    @Test
    fun `중간 구간에서는 노출을 그대로 둔다`() {
        val tuner = AutoExposureTuner()
        assertFalse(tuner.update(0.05f, 10_000))
    }

    @Test
    fun `노출은 최소값 아래로 내려가지 않는다`() {
        val tuner = AutoExposureTuner()
        repeat(20) { tuner.update(0.5f, 10_000L + it * 1_000L) }
        assertEquals(-2f, tuner.ev, 1e-4f)
    }

    @Test
    fun `조금 기운 것만 보정한다`() {
        assertFalse(StraightenMath.shouldStraighten(0.2f))
        assertTrue(StraightenMath.shouldStraighten(-3f))
        assertFalse(StraightenMath.shouldStraighten(25f))
    }

    @Test
    fun `돌리지 않으면 자르지 않는다`() {
        assertEquals(1f, StraightenMath.cropScale(4000, 3000, 0f), 1e-4f)
    }

    @Test
    fun `돌린 뒤 남는 사각형은 원래 사진 안에 들어간다`() {
        val scale = StraightenMath.cropScale(4000, 3000, 5f)
        val radians = Math.toRadians(5.0)
        val w = 4000 * scale
        val h = 3000 * scale
        assertTrue(w * Math.cos(radians) + h * Math.sin(radians) <= 4000.01)
        assertTrue(w * Math.sin(radians) + h * Math.cos(radians) <= 3000.01)
        assertTrue(scale > 0.85f)
    }
}
