package com.aiguidecamera.guide.rules

import com.aiguidecamera.guide.TestFrames.frame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SkyLinePlacementRuleTest {

    private val rule = SkyLinePlacementRule()

    @Test
    fun `수평선이 3분할선에 있으면 통과`() {
        assertNull(rule.check(frame(skyLine = 0.33f)))
        assertNull(rule.check(frame(skyLine = 0.67f)))
    }

    @Test
    fun `경계를 못 찾으면 판단하지 않는다`() {
        assertNull(rule.check(frame(skyLine = null)))
    }

    @Test
    fun `가운데보다 조금 위면 폰을 숙이라고 한다`() {
        assertEquals(HintType.DOWN, rule.check(frame(skyLine = 0.46f))?.hint)
    }

    @Test
    fun `가운데보다 조금 아래면 폰을 들라고 한다`() {
        assertEquals(HintType.UP, rule.check(frame(skyLine = 0.55f))?.hint)
    }
}
