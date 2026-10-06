package com.aiguidecamera.guide.rules

import com.aiguidecamera.analysis.BodyPart
import com.aiguidecamera.guide.TestFrames
import com.aiguidecamera.guide.TestFrames.frame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CameraHeightRuleTest {

    private val rule = CameraHeightRule()

    @Test
    fun `사람이 없으면 피치가 커도 판정하지 않는다`() {
        assertNull(rule.check(frame(pitch = 30f)))
    }

    @Test
    fun `정면에서 잘 찍은 전신은 통과`() {
        assertNull(rule.check(frame(landmarks = TestFrames.goodFullBody(), pitch = 5f)))
    }

    @Test
    fun `피치가 기준보다 크면 폰을 내리라고 한다`() {
        val issue = rule.check(frame(landmarks = TestFrames.upperBodyOnly(), pitch = 15f))
        assertEquals(HintType.DOWN, issue?.hint)
        assertEquals(CameraHeightRule.ID, issue?.ruleId)
    }

    @Test
    fun `전신에서 다리가 짧아 보이면 피치가 작아도 문제`() {
        val landmarks = TestFrames.goodFullBody().apply {
            put(BodyPart.LEFT_HIP, TestFrames.visible(0.45f, 0.62f))
            put(BodyPart.RIGHT_HIP, TestFrames.visible(0.55f, 0.62f))
            put(BodyPart.LEFT_ANKLE, TestFrames.visible(0.45f, 0.80f))
            put(BodyPart.RIGHT_ANKLE, TestFrames.visible(0.55f, 0.80f))
        }
        assertEquals(HintType.DOWN, rule.check(frame(landmarks = landmarks, pitch = 0f))?.hint)
    }
}
