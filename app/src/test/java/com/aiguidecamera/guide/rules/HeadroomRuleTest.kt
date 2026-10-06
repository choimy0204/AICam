package com.aiguidecamera.guide.rules

import com.aiguidecamera.guide.TestFrames
import com.aiguidecamera.guide.TestFrames.frame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HeadroomRuleTest {

    private val rule = HeadroomRule()

    @Test
    fun `사람도 얼굴도 없으면 판정하지 않는다`() {
        assertNull(rule.check(frame()))
    }

    @Test
    fun `포즈로 추정한 여백이 범위 안이면 통과`() {
        assertNull(rule.check(frame(landmarks = TestFrames.goodFullBody())))
    }

    @Test
    fun `얼굴 박스 기준 여백이 범위 안이면 통과`() {
        // 정수리 = 0.14 - 0.08 * 0.25 = 0.12
        assertNull(rule.check(frame(faces = listOf(TestFrames.face(top = 0.14f, bottom = 0.22f)))))
    }

    @Test
    fun `머리 위가 빠듯하면 위로 기울이라고 한다`() {
        val issue = rule.check(frame(faces = listOf(TestFrames.face(top = 0.03f, bottom = 0.11f))))
        assertEquals(HintType.UP, issue?.hint)
    }

    @Test
    fun `머리 위 여백이 넓으면 아래로 기울이라고 한다`() {
        val issue = rule.check(frame(faces = listOf(TestFrames.face(top = 0.30f, bottom = 0.40f))))
        assertEquals(HintType.DOWN, issue?.hint)
    }
}
