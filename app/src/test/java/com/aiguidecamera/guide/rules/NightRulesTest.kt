package com.aiguidecamera.guide.rules

import com.aiguidecamera.guide.TestFrames.frame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NightRulesTest {

    @Test
    fun `가만히 있으면 흔들림 통과`() {
        assertNull(SteadyHoldRule().check(frame(gyro = 0.01f)))
    }

    @Test
    fun `조금만 흔들려도 야경에서는 경고`() {
        assertEquals(SteadyHoldRule.ID, SteadyHoldRule().check(frame(gyro = 0.04f))?.ruleId)
    }

    @Test
    fun `작은 불빛 정도는 번짐 통과`() {
        assertNull(NightGlareRule().check(frame(clip = 0.03f)))
    }

    @Test
    fun `날아간 불빛이 많으면 번짐 경고`() {
        assertEquals(NightGlareRule.ID, NightGlareRule().check(frame(clip = 0.2f))?.ruleId)
    }

    @Test
    fun `불빛이 있는 야경은 어둡다고 하지 않는다`() {
        assertNull(LowLightRule().check(frame(frameBrightness = 0.15f)))
    }

    @Test
    fun `거의 까만 화면이면 어둡다고 한다`() {
        assertEquals(LowLightRule.ID, LowLightRule().check(frame(frameBrightness = 0.02f))?.ruleId)
    }
}
