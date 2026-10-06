package com.aiguidecamera.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppVersionTest {

    @Test
    fun `높은 자리 숫자가 크면 새 버전`() {
        assertTrue(AppVersion.isNewer("v1.1", "1.0"))
        assertTrue(AppVersion.isNewer("2.0", "1.9"))
    }

    @Test
    fun `자리 숫자는 문자열이 아닌 숫자로 비교`() {
        assertTrue(AppVersion.isNewer("1.10", "1.9"))
    }

    @Test
    fun `같거나 낮으면 새 버전이 아님`() {
        assertFalse(AppVersion.isNewer("v1.0", "1.0"))
        assertFalse(AppVersion.isNewer("1.0", "1.0.0"))
        assertFalse(AppVersion.isNewer("0.9", "1.0"))
    }

    @Test
    fun `빠진 자리는 0, 숫자 아닌 꼬리는 무시`() {
        assertTrue(AppVersion.isNewer("1.0.1", "1.0"))
        assertFalse(AppVersion.isNewer("1.0-beta", "1.0"))
    }
}
