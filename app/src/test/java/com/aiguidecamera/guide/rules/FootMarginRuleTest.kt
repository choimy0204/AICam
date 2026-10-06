package com.aiguidecamera.guide.rules

import com.aiguidecamera.analysis.BodyPart
import com.aiguidecamera.guide.TestFrames
import com.aiguidecamera.guide.TestFrames.frame
import com.aiguidecamera.guide.TestFrames.visible
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FootMarginRuleTest {

    private val rule = FootMarginRule()

    @Test
    fun `전신이 아니면 판정하지 않는다`() {
        assertNull(rule.check(frame(landmarks = TestFrames.upperBodyOnly())))
    }

    @Test
    fun `발끝이 화면 아래 가까이 있으면 통과`() {
        assertNull(rule.check(frame(landmarks = TestFrames.goodFullBody())))
    }

    @Test
    fun `발 아래 여백이 넓으면 위로 기울이라고 한다`() {
        val landmarks = TestFrames.goodFullBody().apply {
            put(BodyPart.LEFT_ANKLE, visible(0.45f, 0.75f))
            put(BodyPart.RIGHT_ANKLE, visible(0.55f, 0.76f))
        }
        assertEquals(HintType.UP, rule.check(frame(landmarks = landmarks))?.hint)
    }
}
