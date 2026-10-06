package com.aiguidecamera.update

/**
 * "v1.2.3" 같은 버전 문자열을 점으로 나눈 숫자 단위로 비교한다 (업데이트 확인용).
 * 앞의 v는 무시하고, 숫자가 아닌 꼬리("1.2-beta")는 버린다. 빠진 자리는 0으로 본다.
 */
object AppVersion {

    fun isNewer(candidate: String, current: String): Boolean = compare(candidate, current) > 0

    fun compare(a: String, b: String): Int {
        val partsA = parts(a)
        val partsB = parts(b)
        for (i in 0 until maxOf(partsA.size, partsB.size)) {
            val diff = partsA.getOrElse(i) { 0 }.compareTo(partsB.getOrElse(i) { 0 })
            if (diff != 0) return diff
        }
        return 0
    }

    private fun parts(version: String): List<Int> =
        version.trim().removePrefix("v").removePrefix("V")
            .split('.')
            .map { part -> part.takeWhile(Char::isDigit).toIntOrNull() ?: 0 }
}
