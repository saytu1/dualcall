package com.dualnumber.app.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object TimeFmt {
    private val clockFmt = DateTimeFormatter.ofPattern("a h:mm", Locale.KOREAN)
    private val dayFmt = DateTimeFormatter.ofPattern("yyyy년 M월 d일 EEEE", Locale.KOREAN)

    private fun zdt(ms: Long): ZonedDateTime = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault())

    fun clock(ms: Long): String = zdt(ms).format(clockFmt)

    /** 목록용: 오늘이면 시각, 어제면 "어제", 올해면 "10월 21일" */
    fun short(ms: Long): String {
        val t = zdt(ms)
        val d = t.toLocalDate()
        val today = LocalDate.now(ZoneId.systemDefault())
        return when {
            d == today -> t.format(clockFmt)
            d == today.minusDays(1) -> "어제"
            d.year == today.year -> "${d.monthValue}월 ${d.dayOfMonth}일"
            else -> "${d.year}. ${d.monthValue}. ${d.dayOfMonth}."
        }
    }

    fun dayHeader(ms: Long): String = zdt(ms).format(dayFmt)
    fun dayKey(ms: Long): String = zdt(ms).toLocalDate().toString()
    fun sameDay(a: Long, b: Long): Boolean = zdt(a).toLocalDate() == zdt(b).toLocalDate()

    fun duration(sec: Long): String = when {
        sec <= 0 -> ""
        sec < 60 -> "${sec}초"
        sec < 3600 -> "${sec / 60}분 ${sec % 60}초"
        else -> "${sec / 3600}시간 ${(sec % 3600) / 60}분"
    }
}
