package com.aiguidecamera.render

import org.junit.Assert.assertEquals
import org.junit.Test

class FrameRateMeterTest {

    @Test
    fun `1초 동안 30프레임이면 30fps`() {
        val meter = FrameRateMeter()
        val frameNs = 1_000_000_000L / 30 + 1
        for (i in 0..30) meter.onFrame(1_000L + i * frameNs)
        assertEquals(30f, meter.fps, 0.1f)
    }

    @Test
    fun `1초가 지나기 전에는 0`() {
        val meter = FrameRateMeter()
        meter.onFrame(1_000L)
        meter.onFrame(500_000_000L)
        assertEquals(0f, meter.fps, 0f)
    }
}
