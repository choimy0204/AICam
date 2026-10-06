package com.aiguidecamera.capture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BestShotSelectorTest {

    @Test
    fun `눈 상태가 같으면 가장 선명한 사진`() {
        val scores = listOf(ShotScore(100f, 0.9f), ShotScore(300f, 0.9f), ShotScore(200f, 0.9f))
        assertEquals(1, BestShotSelector.bestIndex(scores))
    }

    @Test
    fun `조금 덜 선명해도 눈 뜬 사진이 눈 감은 사진을 이긴다`() {
        val scores = listOf(ShotScore(300f, 0.1f), ShotScore(270f, 0.95f), ShotScore(250f, 0.9f))
        assertEquals(1, BestShotSelector.bestIndex(scores))
    }

    @Test
    fun `얼굴을 못 찾은 사진은 눈 점수 0`() {
        val scores = listOf(ShotScore(300f, null), ShotScore(250f, 0.8f))
        assertEquals(1, BestShotSelector.bestIndex(scores))
    }

    @Test
    fun `눈 정보가 없으면 선명도만 본다`() {
        val scores = listOf(ShotScore(10f, null), ShotScore(5f, null), ShotScore(30f, null))
        assertEquals(2, BestShotSelector.bestIndex(scores))
    }

    @Test
    fun `모두 0이어도 하나는 고른다`() {
        val index = BestShotSelector.bestIndex(listOf(ShotScore(0f, null), ShotScore(0f, null)))
        assertTrue(index in 0..1)
    }
}
