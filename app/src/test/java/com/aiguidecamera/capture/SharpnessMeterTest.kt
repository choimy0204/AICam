package com.aiguidecamera.capture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SharpnessMeterTest {

    private val size = 32

    private fun image(pixel: (x: Int, y: Int) -> Int) = IntArray(size * size) { i -> pixel(i % size, i / size) }

    @Test
    fun `평평한 이미지는 분산 0`() {
        assertEquals(0f, SharpnessMeter.laplacianVariance(image { _, _ -> 128 }, size, size), 1e-6f)
    }

    @Test
    fun `또렷한 경계가 흐린 경계보다 선명하다`() {
        val sharp = image { x, _ -> if (x < size / 2) 0 else 255 }
        val blurred = image { x, _ -> (x * 255) / (size - 1) }
        val sharpScore = SharpnessMeter.laplacianVariance(sharp, size, size)
        val blurredScore = SharpnessMeter.laplacianVariance(blurred, size, size)
        assertTrue("sharp=$sharpScore blurred=$blurredScore", sharpScore > blurredScore)
    }

    @Test
    fun `너무 작은 이미지는 0`() {
        assertEquals(0f, SharpnessMeter.laplacianVariance(IntArray(4), 2, 2), 0f)
    }
}
