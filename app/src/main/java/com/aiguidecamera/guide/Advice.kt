package com.aiguidecamera.guide

import com.aiguidecamera.guide.rules.Issue

/** UI에 보여줄 조언 상태. 안정화를 거친 결과다. */
sealed interface Advice {
    /** 아직 판단이 안정되지 않음 (아무것도 표시하지 않는다). */
    data object Pending : Advice

    /** 모든 규칙 통과. */
    data object Good : Advice

    /** 확정된 문제 중 우선순위가 가장 높은 하나. */
    data class Problem(val issue: Issue) : Advice
}
