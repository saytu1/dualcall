package com.dualnumber.app.data

import android.content.Context

data class Config(
    val mainNumber: String = "",
    val dualNumber: String = "",
    /** 듀얼 발신 기호: 안드로이드 '#', 아이폰 '*' */
    val suffix: Char = '#',
    /** true: 바로 걸기, false: 다이얼러에 번호만 입력 */
    val directCall: Boolean = true,
)

class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("dual_prefs", Context.MODE_PRIVATE)

    var onboarded: Boolean
        get() = sp.getBoolean("onboarded", false)
        set(value) {
            sp.edit().putBoolean("onboarded", value).apply()
        }

    fun load(): Config = Config(
        mainNumber = sp.getString("main", "") ?: "",
        dualNumber = sp.getString("dual", "") ?: "",
        suffix = (sp.getString("suffix", "#") ?: "#").firstOrNull() ?: '#',
        directCall = sp.getBoolean("direct", true),
    )

    fun save(c: Config) {
        sp.edit()
            .putString("main", c.mainNumber)
            .putString("dual", c.dualNumber)
            .putString("suffix", c.suffix.toString())
            .putBoolean("direct", c.directCall)
            .apply()
    }
}
