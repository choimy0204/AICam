package com.aiguidecamera.camera

import com.aiguidecamera.guide.GuideConstants
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** 수평 자동 보정 계산: 보정할 만큼 기울었는지, 돌린 뒤 빈 모서리 없이 남길 수 있는 크기는 얼마인지. */
object StraightenMath {

    /** 너무 작은 기울기는 티가 안 나고, 너무 크면 일부러 기울인 것으로 보고 건드리지 않는다. */
    fun shouldStraighten(rollDeg: Float): Boolean =
        abs(rollDeg) in GuideConstants.STRAIGHTEN_MIN_DEG..GuideConstants.STRAIGHTEN_MAX_DEG

    /** [angleDeg]만큼 돌린 [width]x[height] 사진 안에 들어가는, 같은 비율의 가운데 사각형 배율 (0~1]. */
    fun cropScale(width: Int, height: Int, angleDeg: Float): Float {
        val radians = Math.toRadians(abs(angleDeg).toDouble())
        val c = cos(radians)
        val s = sin(radians)
        val w = width.toDouble()
        val h = height.toDouble()
        return min(w / (w * c + h * s), h / (w * s + h * c)).toFloat()
    }
}
