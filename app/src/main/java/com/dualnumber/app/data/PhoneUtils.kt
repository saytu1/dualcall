package com.dualnumber.app.data

/** 어느 내 번호로 주고받은 기록인지 */
enum class Line { MAIN, DUAL }

object PhoneUtils {

    /** 공백·하이픈·괄호 등을 지우고 숫자와 '+', '#', '*' 만 남긴다. */
    fun clean(raw: String?): String =
        raw.orEmpty()
            .replace("%23", "#", ignoreCase = true)
            .replace("%2A", "*", ignoreCase = true)
            .filter { it.isDigit() || it == '+' || it == '#' || it == '*' }

    /**
     * 상대 번호 끝에 '#'(안드로이드) 또는 '*'(아이폰)가 붙어 있으면
     * 내 듀얼번호로 주고받은 기록으로 본다.
     */
    fun lineOf(raw: String?): Line {
        val c = clean(raw)
        if (c.length < 2) return Line.MAIN
        val last = c.last()
        val body = c.dropLast(1)
        val isMarker = last == '#' || last == '*'
        return if (isMarker && body.last().isDigit()) Line.DUAL else Line.MAIN
    }

    /** 비교·연락처 매칭용 키: 숫자만 남기고 +82 는 0 으로 바꾼다. */
    fun key(raw: String?): String {
        val c = clean(raw)
        val plus = c.startsWith("+")
        var d = c.filter { it.isDigit() }
        if (d.startsWith("82") && (plus || d.length >= 11)) d = "0" + d.substring(2)
        return d
    }

    /** 실제로 걸거나 보낼 번호. 듀얼이면 뒤에 기호를 붙인다. */
    fun dial(key: String, line: Line, suffix: Char): String =
        if (line == Line.DUAL) key + suffix else key

    /** 보기 좋은 형태: 010-1234-5678 */
    fun format(key: String): String {
        val d = key
        return when {
            d.startsWith("02") && d.length == 10 -> "${d.substring(0, 2)}-${d.substring(2, 6)}-${d.substring(6)}"
            d.startsWith("02") && d.length == 9 -> "${d.substring(0, 2)}-${d.substring(2, 5)}-${d.substring(5)}"
            d.length == 11 -> "${d.substring(0, 3)}-${d.substring(3, 7)}-${d.substring(7)}"
            d.length == 10 -> "${d.substring(0, 3)}-${d.substring(3, 6)}-${d.substring(6)}"
            d.length == 8 && (d.startsWith("15") || d.startsWith("16") || d.startsWith("18")) ->
                "${d.substring(0, 4)}-${d.substring(4)}"
            else -> d
        }
    }
}
