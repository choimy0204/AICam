package com.aiguidecamera.guide.rules

import com.aiguidecamera.guide.TestFrames
import com.aiguidecamera.guide.TestFrames.frame
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SubjectPlacementRuleTest {

    private val rule = SubjectPlacementRule()

    private fun faceAt(centerX: Float) =
        frame(faces = listOf(TestFrames.face(top = 0.2f, bottom = 0.5f, left = centerX - 0.1f, right = centerX + 0.1f)))

    @Test
    fun `사람이 없으면 판정하지 않는다`() {
        assertNull(rule.check(frame()))
    }

    @Test
    fun `가운데나 3분할선이면 통과`() {
        assertNull(rule.check(faceAt(0.50f)))
        assertNull(rule.check(faceAt(0.35f)))
        assertNull(rule.check(faceAt(0.68f)))
    }

    @Test
    fun `어중간한 위치나 가장자리면 맞추라고 한다`() {
        assertNotNull(rule.check(faceAt(0.42f)))
        assertNotNull(rule.check(faceAt(0.15f)))
    }

    @Test
    fun `얼굴이 없으면 코 위치로 본다`() {
        assertNull(rule.check(frame(landmarks = TestFrames.goodFullBody())))
    }
}
