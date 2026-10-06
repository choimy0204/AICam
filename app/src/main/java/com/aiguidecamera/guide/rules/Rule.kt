package com.aiguidecamera.guide.rules

import com.aiguidecamera.analysis.FrameAnalysisResult

/** 구도·빛 규칙 하나. 분석 결과만 보고 문제가 있으면 [Issue], 없으면 null을 돌려준다. */
interface Rule {
    val id: String

    /** 낮을수록 먼저 보여준다. */
    val priority: Int

    fun check(result: FrameAnalysisResult): Issue?
}

data class Issue(val ruleId: String, val priority: Int, val message: String, val hint: HintType)

/** 조언 배너에 함께 그릴 화살표. 폰을 어느 쪽으로 움직이거나 돌릴지. */
enum class HintType { UP, DOWN, BACK, ROTATE_LEFT, ROTATE_RIGHT, NONE }
