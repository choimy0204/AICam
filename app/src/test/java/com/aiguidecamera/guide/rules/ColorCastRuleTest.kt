package com.aiguidecamera.guide.rules

import com.aiguidecamera.guide.TestFrames.frame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ColorCastRuleTest {

    private val rule = ColorCastRule()

    @Test
    fun `중성 조명은 통과`() {
        assertNull(rule.check(frame(red = 0.5f, blue = 0.45f)))
    }

    @Test
    fun `누런 조명 감지`() {
        assertEquals(ColorCastRule.ID, rule.check(frame(red = 0.7f, blue = 0.4f))?.ruleId)
    }

    @Test
    fun `푸른 조명 감지`() {
        assertEquals(ColorCastRule.ID, rule.check(frame(red = 0.3f, blue = 0.5f))?.ruleId)
    }

    @Test
    fun `파랑이 거의 없으면 판정하지 않는다`() {
        assertNull(rule.check(frame(red = 0.3f, blue = 0.01f)))
    }
}
