package com.aiguidecamera.guide

import com.aiguidecamera.guide.rules.HintType
import com.aiguidecamera.guide.rules.Issue
import org.junit.Assert.assertEquals
import org.junit.Test

class IssueStabilizerTest {

    private val stabilizer = IssueStabilizer(confirmFrames = 5, clearFrames = 5)
    private val horizon = Issue("horizon", priority = 5, message = "기울었어요", hint = HintType.ROTATE_LEFT)
    private val headroom = Issue("headroom", priority = 3, message = "여백", hint = HintType.UP)

    private fun feed(times: Int, vararg issues: Issue): Advice {
        var advice: Advice = Advice.Pending
        repeat(times) { advice = stabilizer.update(issues.toList()) }
        return advice
    }

    @Test
    fun `연속 5프레임이 돼야 문제를 확정한다`() {
        assertEquals(Advice.Pending, feed(4, horizon))
        assertEquals(Advice.Problem(horizon), feed(1, horizon))
    }

    @Test
    fun `확정된 문제는 연속 5프레임 사라져야 해제된다`() {
        feed(5, horizon)
        assertEquals(Advice.Problem(horizon), feed(4))
        assertEquals(Advice.Good, feed(1))
    }

    @Test
    fun `깜빡이는 문제는 확정되지 않는다`() {
        repeat(10) {
            assertEquals(Advice.Pending, stabilizer.update(listOf(horizon)))
            assertEquals(Advice.Pending, stabilizer.update(emptyList()))
        }
    }

    @Test
    fun `확정된 문제가 여럿이면 우선순위가 높은 하나만`() {
        assertEquals(Advice.Problem(headroom), feed(5, horizon, headroom))
    }

    @Test
    fun `문제가 없는 프레임이 5번 이어지면 좋아요`() {
        assertEquals(Advice.Pending, feed(4))
        assertEquals(Advice.Good, feed(1))
    }

    @Test
    fun `초기화하면 확정된 문제를 잊는다`() {
        feed(5, horizon)
        stabilizer.reset()
        assertEquals(Advice.Pending, feed(1))
    }
}
