package com.aiguidecamera.analysis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SkyLineFinderTest {

    private fun bands(boundary: Int, sky: Float, ground: Float) =
        FloatArray(24) { if (it < boundary) sky else ground }

    @Test
    fun `밝은 하늘과 어두운 땅의 경계를 찾는다`() {
        assertEquals(8f / 24f, SkyLineFinder.find(bands(8, sky = 0.8f, ground = 0.3f))!!, 0.001f)
    }

    @Test
    fun `아래쪽 3분할 근처 경계도 찾는다`() {
        assertEquals(16f / 24f, SkyLineFinder.find(bands(16, sky = 0.7f, ground = 0.35f))!!, 0.001f)
    }

    @Test
    fun `밝기가 고르면 경계 없음`() {
        assertNull(SkyLineFinder.find(FloatArray(24) { 0.5f }))
    }

    @Test
    fun `위가 더 어두우면 하늘로 보지 않는다`() {
        assertNull(SkyLineFinder.find(bands(12, sky = 0.2f, ground = 0.7f)))
    }

    @Test
    fun `차이가 작으면 경계로 인정하지 않는다`() {
        assertNull(SkyLineFinder.find(bands(12, sky = 0.55f, ground = 0.5f)))
    }
}
