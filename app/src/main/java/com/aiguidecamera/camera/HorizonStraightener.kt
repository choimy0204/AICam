package com.aiguidecamera.camera

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import kotlin.math.roundToInt

/** 찍는 순간의 기울기만큼 사진을 반대로 돌리고, 빈 모서리가 안 보이게 가운데를 같은 비율로 잘라낸다. */
object HorizonStraightener {

    /** 폰이 반시계로 [rollDeg]만큼 기울었으면 사진 속 수평선은 시계 방향으로 기울어 있으므로 반시계로 되돌린다. 입력은 해제한다. */
    fun straighten(photo: Bitmap, rollDeg: Float): Bitmap {
        val scale = StraightenMath.cropScale(photo.width, photo.height, rollDeg)
        val outWidth = (photo.width * scale).roundToInt()
        val outHeight = (photo.height * scale).roundToInt()
        val matrix = Matrix().apply {
            postTranslate(-photo.width / 2f, -photo.height / 2f)
            postRotate(-rollDeg)
            postTranslate(outWidth / 2f, outHeight / 2f)
        }
        val straightened = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)
        Canvas(straightened).drawBitmap(photo, matrix, Paint(Paint.FILTER_BITMAP_FLAG))
        photo.recycle()
        return straightened
    }
}
