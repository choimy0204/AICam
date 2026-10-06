package com.aiguidecamera.analysis

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * 회전 벡터 센서로 기울기(roll)·내려다보는 각도(pitch)를, 자이로로 흔들림을 읽는다.
 * 센서 스레드가 쓰고 분석 스레드가 읽으므로 값은 @Volatile 필드로 둔다. [start]/[stop]은 화면 수명주기에 맞춰 부른다.
 */
class SensorReader(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val gyroSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val rotationMatrix = FloatArray(9)

    @Volatile var rollDeg = 0f
        private set

    @Volatile var deviceRollDeg = 0f
        private set

    @Volatile var pitchDeg = 0f
        private set

    @Volatile private var gyroPeak = 0f

    fun start() {
        rotationSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
        gyroSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    /** 직전 호출 이후 자이로 각속도 크기의 최댓값을 돌려주고 0으로 되돌린다. */
    fun consumeGyroPeak(): Float {
        val peak = gyroPeak
        gyroPeak = 0f
        return peak
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR -> updateOrientation(event.values)
            Sensor.TYPE_GYROSCOPE -> {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                val magnitude = sqrt(x * x + y * y + z * z)
                if (magnitude > gyroPeak) gyroPeak = magnitude
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit

    private fun updateOrientation(rotationVector: FloatArray) {
        SensorManager.getRotationMatrixFromVector(rotationMatrix, rotationVector)
        // R[6..8] = 기기 좌표계로 본 "위쪽" 방향. 후면 카메라는 기기 -Z를 보므로 R[8]이 클수록 아래를 본다.
        pitchDeg = Math.toDegrees(asin(rotationMatrix[8].coerceIn(-1f, 1f).toDouble())).toFloat()
        val rawRoll = Math.toDegrees(atan2(rotationMatrix[6], rotationMatrix[7]).toDouble()).toFloat()
        deviceRollDeg = rawRoll
        // 세로·가로 어느 쪽으로 잡았든 가장 가까운 90도 방향 기준 기울기로 만든다.
        rollDeg = rawRoll - (rawRoll / QUARTER_TURN_DEG).roundToInt() * QUARTER_TURN_DEG
    }

    private companion object {
        const val QUARTER_TURN_DEG = 90f
    }
}
