package com.dualnumber.app.data

import com.dualnumber.app.util.TimeFmt

enum class Kind { CALL, SMS }
enum class Direction { IN, OUT, MISSED, REJECTED }

/** 통화 1건 또는 문자 1건 */
data class Event(
    val id: String,
    val kind: Kind,
    val direction: Direction,
    val line: Line,
    val key: String,
    val rawNumber: String,
    val time: Long,
    val durationSec: Long = 0,
    val body: String? = null,
)

/** 같은 상대 + 같은 내 번호로 주고받은 기록 묶음 (최신순) */
data class Conversation(
    val key: String,
    val line: Line,
    val name: String?,
    val events: List<Event>,
) {
    val id: String get() = "$key|${line.name}"
    val latest: Event get() = events.first()
}

fun groupConversations(events: List<Event>, contacts: Map<String, String>): List<Conversation> =
    events.groupBy { it.key to it.line }
        .map { (k, evs) -> Conversation(k.first, k.second, contacts[k.first], evs) }
        .sortedByDescending { it.latest.time }

fun displayName(key: String, name: String?): String =
    name ?: if (key.isEmpty()) "발신번호 표시제한" else PhoneUtils.format(key)

fun callLabel(e: Event): String {
    val dur = TimeFmt.duration(e.durationSec)
    return when (e.direction) {
        Direction.IN -> if (dur.isEmpty()) "받은 전화" else "받은 전화 $dur"
        Direction.OUT -> if (dur.isEmpty()) "건 전화 · 연결 안 됨" else "건 전화 $dur"
        Direction.MISSED -> "부재중 전화"
        Direction.REJECTED -> "거절·차단된 전화"
    }
}

fun previewText(e: Event): String = when (e.kind) {
    Kind.CALL -> {
        val arrow = if (e.direction == Direction.OUT) "↗" else "↙"
        "$arrow ${callLabel(e)}"
    }
    Kind.SMS -> {
        val body = e.body.orEmpty().replace('\n', ' ').trim().ifEmpty { "(내용 없음)" }
        if (e.direction == Direction.OUT) "나: $body" else body
    }
}
