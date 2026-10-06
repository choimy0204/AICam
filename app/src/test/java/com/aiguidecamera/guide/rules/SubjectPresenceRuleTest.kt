package com.aiguidecamera.guide.rules

import com.aiguidecamera.analysis.BodyPart
import com.aiguidecamera.guide.TestFrames
import com.aiguidecamera.guide.TestFrames.frame
import com.aiguidecamera.guide.TestFrames.visible
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SubjectPresenceRuleTest {

    private val rule = SubjectPresenceRule()

    @Test
    fun `사람도 얼굴도 없으면 사람을 넣으라고 한다`() {
        assertEquals("사람이 화면에 들어오게 해 주세요", rule.check(frame())?.message)
    }

    @Test
    fun `거울 속처럼 작은 얼굴만 있으면 너무 작다고 한다`() {
        val issue = rule.check(frame(faces = listOf(TestFrames.face(top = 0.40f, bottom = 0.44f))))
        assertNotNull(issue)
        assertEquals(0, issue?.priority)
    }

    @Test
    fun `얼굴이 충분히 크면 통과`() {
        assertNull(rule.check(frame(faces = listOf(TestFrames.face(top = 0.14f, bottom = 0.26f)))))
    }

    @Test
    fun `구도가 맞은 전신은 얼굴이 작아도 통과`() {
        val face = TestFrames.face(top = 0.14f, bottom = 0.19f)
        assertNull(rule.check(frame(landmarks = TestFrames.goodFullBody(), faces = listOf(face))))
    }

    @Test
    fun `멀리 있는 작은 전신은 너무 작다고 한다`() {
        val landmarks = TestFrames.goodFullBody().apply {
            put(BodyPart.NOSE, visible(0.50f, 0.50f))
            put(BodyPart.LEFT_ANKLE, visible(0.48f, 0.70f))
            put(BodyPart.RIGHT_ANKLE, visible(0.52f, 0.70f))
        }
        assertNotNull(rule.check(frame(landmarks = landmarks)))
    }
}
