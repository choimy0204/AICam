package com.aiguidecamera.guide.rules

import com.aiguidecamera.guide.TestFrames.frame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HorizonRuleTest {

    private val rule = HorizonRule(priority = 5)

    @Test
    fun `허용 오차 안이면 통과`() {
        assertNull(rule.check(frame(roll = 1.5f)))
    }

    @Test
    fun `롤이 양수면 오른쪽으로 돌리라고 한다`() {
        val issue = rule.check(frame(roll = 4f))
        assertEquals(HintType.ROTATE_RIGHT, issue?.hint)
        assertEquals(5, issue?.priority)
    }

    @Test
    fun `롤이 음수면 왼쪽으로 돌리라고 한다`() {
        assertEquals(HintType.ROTATE_LEFT, rule.check(frame(roll = -4f))?.hint)
    }

    @Test
    fun `바닥을 내려다볼 때는 판정하지 않는다`() {
        assertNull(rule.check(frame(roll = 20f, pitch = 85f)))
    }
}
