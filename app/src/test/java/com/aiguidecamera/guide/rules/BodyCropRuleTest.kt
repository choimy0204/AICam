package com.aiguidecamera.guide.rules

import com.aiguidecamera.analysis.BodyPart
import com.aiguidecamera.guide.TestFrames
import com.aiguidecamera.guide.TestFrames.frame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BodyCropRuleTest {

    private val rule = BodyCropRule()

    @Test
    fun `잘 찍은 전신은 통과`() {
        assertNull(rule.check(frame(landmarks = TestFrames.goodFullBody())))
    }

    @Test
    fun `무릎 위 상반신은 통과`() {
        assertNull(rule.check(frame(landmarks = TestFrames.upperBodyOnly())))
    }

    @Test
    fun `무릎은 보이는데 발목이 없으면 발끝 잘림`() {
        val landmarks = TestFrames.goodFullBody().apply {
            remove(BodyPart.LEFT_ANKLE)
            remove(BodyPart.RIGHT_ANKLE)
        }
        val issue = rule.check(frame(landmarks = landmarks))
        assertEquals(HintType.BACK, issue?.hint)
        assertTrue(issue!!.message.contains("발끝"))
    }

    @Test
    fun `신뢰도 낮은 발목은 안 보이는 것으로 친다`() {
        val landmarks = TestFrames.goodFullBody().apply {
            put(BodyPart.LEFT_ANKLE, TestFrames.hidden(0.45f, 0.86f))
            put(BodyPart.RIGHT_ANKLE, TestFrames.hidden(0.55f, 0.86f))
        }
        assertTrue(rule.check(frame(landmarks = landmarks))!!.message.contains("발끝"))
    }

    @Test
    fun `발목이 화면 하단 경계에 걸리면 발끝 잘림`() {
        val landmarks = TestFrames.goodFullBody().apply {
            put(BodyPart.LEFT_ANKLE, TestFrames.visible(0.45f, 0.99f))
        }
        assertTrue(rule.check(frame(landmarks = landmarks))!!.message.contains("발끝"))
    }

    @Test
    fun `팔꿈치가 화면 옆 끝에 걸리면 관절 잘림`() {
        val landmarks = TestFrames.goodFullBody().apply {
            put(BodyPart.LEFT_ELBOW, TestFrames.visible(0.01f, 0.38f))
        }
        val issue = rule.check(frame(landmarks = landmarks))
        assertTrue(issue!!.message.contains("관절"))
    }
}
