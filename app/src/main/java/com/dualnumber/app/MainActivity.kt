package com.dualnumber.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import com.dualnumber.app.data.Line
import com.dualnumber.app.ui.App
import com.dualnumber.app.ui.DualTheme

class MainActivity : ComponentActivity() {

    /** 알림을 눌렀을 때 열 대화 (번호키, 내 번호 구분) */
    private val openRequest = mutableStateOf<Pair<String, Line>?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) handleIntent(intent)
        setContent {
            DualTheme {
                App(openRequest = openRequest.value, onOpenConsumed = { openRequest.value = null })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(i: Intent?) {
        val key = i?.getStringExtra(EXTRA_KEY) ?: return
        val line = runCatching { Line.valueOf(i.getStringExtra(EXTRA_LINE).orEmpty()) }.getOrDefault(Line.DUAL)
        openRequest.value = key to line
    }

    companion object {
        const val EXTRA_KEY = "open_key"
        const val EXTRA_LINE = "open_line"
    }
}
