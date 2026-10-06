package com.aiguidecamera.guide.rules

import com.aiguidecamera.guide.FoodAngle
import com.aiguidecamera.guide.TestFrames.frame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FoodAngleRuleTest {

    private val topView = FoodAngleRule(FoodAngle.TOP_VIEW)
    private val angled = FoodAngleRule(FoodAngle.ANGLED)

    @Test
    fun `탑뷰 범위 안이면 통과`() {
        assertNull(topView.check(frame(pitch = 87f)))
    }

    @Test
    fun `탑뷰인데 덜 눕혔으면 더 내려다보라고 한다`() {
        assertEquals(HintType.DOWN, topView.check(frame(pitch = 70f))?.hint)
    }

    @Test
    fun `45도 범위 안이면 통과`() {
        assertNull(angled.check(frame(pitch = 45f)))
    }

    @Test
    fun `45도인데 너무 세웠으면 더 기울이라고 한다`() {
        assertEquals(HintType.DOWN, angled.check(frame(pitch = 30f))?.hint)
    }

    @Test
    fun `45도인데 너무 눕혔으면 세우라고 한다`() {
        assertEquals(HintType.UP, angled.check(frame(pitch = 60f))?.hint)
    }
}
