package com.aiguidecamera.guide.rules

import com.aiguidecamera.analysis.BodyPart
import com.aiguidecamera.guide.TestFrames
import com.aiguidecamera.guide.TestFrames.frame
import com.aiguidecamera.guide.TestFrames.visible
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HeadroomRuleTest {

    private val rule = HeadroomRule()

    @Test
    fun `전신이 아니면 판정하지 않는다`() {
        assertNull(rule.check(frame(faces = listOf(TestFrames.face(top = 0.01f, bottom = 0.10f)))))
    }

    @Test
    fun `포즈로 추정한 여백이 충분하면 통과`() {
        assertNull(rule.check(frame(landmarks = TestFrames.goodFullBody())))
    }

    @Test
    fun `전신은 머리 위 여백이 넓어도 통과`() {
        val face = TestFrames.face(top = 0.30f, bottom = 0.36f)
        assertNull(rule.check(frame(landmarks = TestFrames.goodFullBody(), faces = listOf(face))))
    }

    @Test
    fun `전신에서 머리 위가 빠듯하면 위로 기울이라고 한다`() {
        val landmarks = TestFrames.goodFullBody().apply { put(BodyPart.NOSE, visible(0.50f, 0.06f)) }
        val face = TestFrames.face(top = 0.03f, bottom = 0.09f)
        assertEquals(HintType.UP, rule.check(frame(landmarks = landmarks, faces = listOf(face)))?.hint)
    }
}
