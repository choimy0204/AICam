package com.aiguidecamera.guide.rules

import com.aiguidecamera.guide.TestFrames
import com.aiguidecamera.guide.TestFrames.frame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EyeLineRuleTest {

    private val rule = EyeLineRule()

    @Test
    fun `얼굴이 없으면 판정하지 않는다`() {
        assertNull(rule.check(frame()))
    }

    @Test
    fun `전신은 판정하지 않는다`() {
        assertNull(rule.check(frame(landmarks = TestFrames.goodFullBody())))
    }

    @Test
    fun `눈이 위쪽 3분할선 근처면 통과`() {
        // 눈 = 0.20 + 0.30 * 0.4 = 0.32
        assertNull(rule.check(frame(faces = listOf(TestFrames.face(top = 0.20f, bottom = 0.50f)))))
    }

    @Test
    fun `눈이 너무 위에 있으면 위로 기울이라고 한다`() {
        // 눈 = 0.05 + 0.20 * 0.4 = 0.13
        val issue = rule.check(frame(faces = listOf(TestFrames.face(top = 0.05f, bottom = 0.25f))))
        assertEquals(HintType.UP, issue?.hint)
    }

    @Test
    fun `눈이 화면 가운데쯤이면 아래로 기울이라고 한다`() {
        // 눈 = 0.40 + 0.20 * 0.4 = 0.48
        val issue = rule.check(frame(faces = listOf(TestFrames.face(top = 0.40f, bottom = 0.60f))))
        assertEquals(HintType.DOWN, issue?.hint)
    }

    @Test
    fun `상반신은 눈 관절 위치로 본다`() {
        // upperBodyOnly의 눈 높이는 0.17
        assertEquals(HintType.UP, rule.check(frame(landmarks = TestFrames.upperBodyOnly()))?.hint)
    }
}
