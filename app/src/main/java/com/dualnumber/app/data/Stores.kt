package com.dualnumber.app.data

import android.content.Context
import android.util.AtomicFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * 앱이 직접 쌓는 통화 기록. (통화기록 권한 없이, 발신자 확인 서비스가 감지한 것만)
 * 앱 내부 저장소에만 저장되고 외부로 나가지 않는다.
 */
object EventStore {
    private const val MAX = 1000
    private val _events = MutableStateFlow<List<Event>>(emptyList())
    val events: StateFlow<List<Event>> = _events.asStateFlow()

    @Volatile
    private var loaded = false

    private fun file(ctx: Context) = AtomicFile(File(ctx.filesDir, "events.json"))

    @Synchronized
    fun init(ctx: Context) {
        if (loaded) return
        loaded = true
        val list = runCatching {
            val arr = JSONArray(String(file(ctx).readFully(), Charsets.UTF_8))
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Event(
                    id = o.getString("id"),
                    direction = Direction.valueOf(o.getString("dir")),
                    line = Line.valueOf(o.getString("line")),
                    key = o.getString("key"),
                    rawNumber = o.optString("raw"),
                    time = o.getLong("time"),
                )
            }
        }.getOrDefault(emptyList())
        _events.value = list.sortedByDescending { it.time }
    }

    @Synchronized
    fun add(ctx: Context, e: Event) {
        init(ctx)
        _events.value = (listOf(e) + _events.value).take(MAX)
        save(ctx)
    }

    @Synchronized
    fun deleteConversation(ctx: Context, key: String, line: Line) {
        init(ctx)
        _events.value = _events.value.filterNot { it.key == key && it.line == line }
        save(ctx)
    }

    @Synchronized
    fun clear(ctx: Context) {
        init(ctx)
        _events.value = emptyList()
        save(ctx)
    }

    private fun save(ctx: Context) {
        val arr = JSONArray()
        _events.value.forEach { e ->
            arr.put(
                JSONObject()
                    .put("id", e.id)
                    .put("dir", e.direction.name)
                    .put("line", e.line.name)
                    .put("key", e.key)
                    .put("raw", e.rawNumber)
                    .put("time", e.time)
            )
        }
        val f = file(ctx)
        val out = f.startWrite()
        try {
            out.write(arr.toString().toByteArray(Charsets.UTF_8))
            f.finishWrite(out)
        } catch (t: Throwable) {
            f.failWrite(out)
        }
    }
}

/** 번호 → 이름. 연락처 선택기로 고르거나 직접 붙인 이름만 저장 (연락처 권한 불필요) */
object NameStore {
    private val _names = MutableStateFlow<Map<String, String>>(emptyMap())
    val names: StateFlow<Map<String, String>> = _names.asStateFlow()

    @Volatile
    private var loaded = false

    private fun sp(ctx: Context) = ctx.getSharedPreferences("names", Context.MODE_PRIVATE)

    @Synchronized
    fun init(ctx: Context) {
        if (loaded) return
        loaded = true
        _names.value = sp(ctx).all.mapNotNull { (k, v) -> (v as? String)?.let { k to it } }.toMap()
    }

    @Synchronized
    fun set(ctx: Context, key: String, name: String?) {
        if (key.isEmpty()) return
        init(ctx)
        val n = name?.trim().orEmpty()
        if (n.isEmpty()) {
            sp(ctx).edit().remove(key).apply()
            _names.value = _names.value - key
        } else {
            sp(ctx).edit().putString(key, n).apply()
            _names.value = _names.value + (key to n)
        }
    }
}
