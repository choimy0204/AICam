package com.aiguidecamera.guide.rules

import com.aiguidecamera.guide.TestFrames.frame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HighlightClipRuleTest {

    private val rule = HighlightClipRule()

    @Test
    fun `날아간 부분이 적으면 통과`() {
        assertNull(rule.check(frame(clip = 0.05f)))
    }

    @Test
    fun `날아간 부분이 많으면 경고`() {
        assertEquals(HighlightClipRule.ID, rule.check(frame(clip = 0.3f))?.ruleId)
    }
}
