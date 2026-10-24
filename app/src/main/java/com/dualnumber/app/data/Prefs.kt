package com.dualnumber.app.data

import android.content.Context

data class Config(
    val mainNumber: String = "",
    val dualNumber: String = "",
    /** 듀얼 발신 기호: 안드로이드 '#', 아이폰 '*' */
    val suffix: Char = '#',
    /** 듀얼번호로 전화가 오면 알림 */
    val notifyDual: Boolean = true,
    /** 기본번호로 전화가 와도 알림 */
    val notifyMain: Boolean = false,
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
        notifyDual = sp.getBoolean("notify_dual", true),
        notifyMain = sp.getBoolean("notify_main", false),
    )

    fun save(c: Config) {
        sp.edit()
            .putString("main", c.mainNumber)
            .putString("dual", c.dualNumber)
            .putString("suffix", c.suffix.toString())
            .putBoolean("notify_dual", c.notifyDual)
            .putBoolean("notify_main", c.notifyMain)
            .apply()
    }
}
