package com.aiguidecamera.capture

import com.aiguidecamera.analysis.BodyPart
import com.aiguidecamera.analysis.FrameAnalysisResult
import com.aiguidecamera.analysis.Landmark
import com.aiguidecamera.guide.TestFrames.frame
import com.aiguidecamera.guide.TestFrames.goodFullBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoCaptureStateMachineTest {

    private val machine = AutoCaptureStateMachine(holdMs = 700L)
    private var clock = 0L

    /** 100ms 간격 프레임 하나를 넣고 촬영 신호를 돌려준다. */
    private fun step(isReady: Boolean, landmarks: Map<BodyPart, Landmark> = goodFullBody(), pitch: Float = 0f): Boolean {
        val result: FrameAnalysisResult = frame(landmarks = landmarks, pitch = pitch, time = clock)
        clock += 100L
        return machine.onFrame(isReady, result)
    }

    /** [frames]개 프레임 동안 촬영 신호가 몇 번 나왔는지. */
    private fun run(frames: Int, isReady: Boolean, landmarks: Map<BodyPart, Landmark> = goodFullBody(), pitch: Float = 0f): Int {
        var fired = 0
        repeat(frames) { if (step(isReady, landmarks, pitch)) fired++ }
        return fired
    }

    private fun captureOnce() {
        assertEquals(1, run(8, isReady = true))
        machine.onCaptureFinished()
        assertEquals(AutoCaptureState.COOLDOWN, machine.state)
    }

    private fun shifted(dx: Float) = goodFullBody().mapValues { (_, l) -> l.copy(x = l.x + dx) }

    @Test
    fun `조건이 한 번도 충족되지 않으면 절대 찍지 않는다`() {
        assertEquals(0, run(1_000, isReady = false))
        assertEquals(AutoCaptureState.AIMING, machine.state)
    }

    @Test
    fun `700ms 미만으로 유지되면 찍지 않는다`() {
        // 0~600ms: 7프레임, 경과 600ms
        assertEquals(0, run(7, isReady = true))
        assertEquals(AutoCaptureState.STABILIZING, machine.state)
    }

    @Test
    fun `700ms 연속 유지되면 정확히 한 번 찍는다`() {
        assertEquals(0, run(7, isReady = true))
        assertTrue(step(isReady = true))
        assertEquals(AutoCaptureState.CAPTURING, machine.state)
        assertEquals(0, run(20, isReady = true))
    }

    @Test
    fun `중간에 조건이 깨지면 처음부터 다시 잰다`() {
        run(6, isReady = true)
        assertFalse(step(isReady = false))
        assertEquals(AutoCaptureState.AIMING, machine.state)
        assertEquals(0, run(7, isReady = true))
        assertTrue(step(isReady = true))
    }

    @Test
    fun `조건이 깜빡이면 계속 찍지 않는다`() {
        repeat(100) {
            assertEquals(0, run(5, isReady = true))
            assertEquals(0, run(1, isReady = false))
        }
    }

    @Test
    fun `쿨다운 중 같은 구도면 다시 찍지 않는다`() {
        captureOnce()
        assertEquals(0, run(200, isReady = true))
        assertEquals(AutoCaptureState.COOLDOWN, machine.state)
    }

    @Test
    fun `포즈 중심이 크게 옮겨가면 쿨다운이 풀리고 다시 찍는다`() {
        captureOnce()
        assertEquals(0, run(1, isReady = true, landmarks = shifted(0.2f)))
        assertEquals(AutoCaptureState.AIMING, machine.state)
        assertEquals(1, run(10, isReady = true, landmarks = shifted(0.2f)))
    }

    @Test
    fun `포즈가 조금 움직인 정도로는 쿨다운이 유지된다`() {
        captureOnce()
        run(20, isReady = true, landmarks = shifted(0.05f))
        assertEquals(AutoCaptureState.COOLDOWN, machine.state)
    }

    @Test
    fun `피치가 크게 바뀌면 쿨다운이 풀린다`() {
        captureOnce()
        run(1, isReady = true, pitch = 20f)
        assertEquals(AutoCaptureState.AIMING, machine.state)
    }

    @Test
    fun `포즈를 놓친 프레임은 구도 변화로 보지 않는다`() {
        captureOnce()
        run(20, isReady = false, landmarks = emptyMap())
        assertEquals(AutoCaptureState.COOLDOWN, machine.state)
    }

    @Test
    fun `초기화하면 조준 상태로 돌아간다`() {
        captureOnce()
        machine.reset()
        assertEquals(AutoCaptureState.AIMING, machine.state)
    }
}
