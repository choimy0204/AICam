package com.aiguidecamera.guide

import com.aiguidecamera.guide.rules.Issue

/**
 * 규칙 결과의 깜빡임을 막는 히스테리시스. 같은 문제가 [confirmFrames] 연속이면 확정, [clearFrames] 연속 사라지면 해제.
 * 확정된 문제 중 우선순위가 가장 높은 1개, 없으면 "좋아요"(문제 없는 프레임이 [clearFrames] 연속일 때)를 돌려준다.
 */
class IssueStabilizer(
    private val confirmFrames: Int = GuideConstants.ISSUE_CONFIRM_FRAMES,
    private val clearFrames: Int = GuideConstants.ISSUE_CLEAR_FRAMES,
) {
    private class Track {
        var presentStreak = 0
        var absentStreak = 0
        var confirmed = false
        var seenThisFrame = false
        var latest: Issue? = null
    }

    private val tracks = HashMap<String, Track>()
    private var cleanStreak = 0

    fun update(issues: List<Issue>): Advice {
        for (track in tracks.values) track.seenThisFrame = false
        for (issue in issues) {
            val track = tracks.getOrPut(issue.ruleId) { Track() }
            track.seenThisFrame = true
            track.latest = issue
            track.presentStreak++
            track.absentStreak = 0
            if (track.presentStreak >= confirmFrames) track.confirmed = true
        }
        for (track in tracks.values) {
            if (track.seenThisFrame) continue
            track.absentStreak++
            track.presentStreak = 0
            if (track.absentStreak >= clearFrames) track.confirmed = false
        }
        cleanStreak = if (issues.isEmpty()) cleanStreak + 1 else 0

        var top: Issue? = null
        for (track in tracks.values) {
            val issue = track.latest ?: continue
            if (track.confirmed && (top == null || issue.priority < top.priority)) top = issue
        }
        return when {
            top != null -> Advice.Problem(top)
            cleanStreak >= clearFrames -> Advice.Good
            else -> Advice.Pending
        }
    }

    /** 모드 전환 등으로 규칙 묶음이 바뀌면 이전 기록을 버린다. */
    fun reset() {
        tracks.clear()
        cleanStreak = 0
    }
}
