package com.aiguidecamera.guide.rules

import com.aiguidecamera.guide.TestFrames.frame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BacklightRuleTest {

    private val rule = BacklightRule()

    @Test
    fun `얼굴이 없으면 판정하지 않는다`() {
        assertNull(rule.check(frame(frameBrightness = 0.8f)))
    }

    @Test
    fun `얼굴이 배경만큼 밝으면 통과`() {
        assertNull(rule.check(frame(faceBrightness = 0.4f, frameBrightness = 0.5f)))
    }

    @Test
    fun `얼굴이 배경보다 많이 어두우면 역광`() {
        val issue = rule.check(frame(faceBrightness = 0.25f, frameBrightness = 0.6f))
        assertEquals(BacklightRule.ID, issue?.ruleId)
    }

    @Test
    fun `화면 전체가 너무 어두우면 판정하지 않는다`() {
        assertNull(rule.check(frame(faceBrightness = 0.01f, frameBrightness = 0.03f)))
    }
}
