package com.aiguidecamera.render

/**
 * 1초 단위로 프레임 수를 세어 fps를 낸다. 프리뷰 프레임레이트 점검(디버그 오버레이)용.
 * [onFrame]은 한 스레드(GL)에서만 부르고, [fps]는 어느 스레드에서 읽어도 된다.
 */
class FrameRateMeter {

    @Volatile var fps: Float = 0f
        private set

    private var windowStartNs = 0L
    private var frameCount = 0

    fun onFrame(nowNs: Long) {
        // 첫 프레임은 구간의 시작점일 뿐 구간 안의 프레임으로 세지 않는다.
        if (windowStartNs == 0L) {
            windowStartNs = nowNs
            return
        }
        frameCount++
        val elapsedNs = nowNs - windowStartNs
        if (elapsedNs >= WINDOW_NS) {
            fps = frameCount * NS_PER_SECOND / elapsedNs.toFloat()
            frameCount = 0
            windowStartNs = nowNs
        }
    }

    private companion object {
        const val NS_PER_SECOND = 1_000_000_000f
        const val WINDOW_NS = 1_000_000_000L
    }
}
