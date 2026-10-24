package com.dualnumber.app.data

enum class Direction { IN, OUT }

/** 앱이 감지한 통화 1건 (설치 이후부터 기록) */
data class Event(
    val id: String,
    val direction: Direction,
    val line: Line,
    val key: String,
    /** 시스템이 넘겨준 원본 번호 (예: 01099990000#) — '#' 전달 여부 확인용 */
    val rawNumber: String,
    val time: Long,
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

fun groupConversations(events: List<Event>, names: Map<String, String>): List<Conversation> =
    events.groupBy { it.key to it.line }
        .map { (k, evs) -> Conversation(k.first, k.second, names[k.first], evs.sortedByDescending { it.time }) }
        .sortedByDescending { it.latest.time }

fun displayName(key: String, name: String?): String =
    name ?: if (key.isEmpty()) "발신번호 표시제한" else PhoneUtils.format(key)

fun callLabel(e: Event): String = if (e.direction == Direction.IN) "걸려온 전화" else "건 전화"

fun previewText(e: Event): String {
    val arrow = if (e.direction == Direction.OUT) "↗" else "↙"
    return "$arrow ${callLabel(e)}"
}
