package com.dualnumber.app.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.CallLog
import android.provider.ContactsContract
import androidx.core.content.ContextCompat

val REQUIRED_PERMISSIONS = arrayOf(
    Manifest.permission.READ_CALL_LOG,
    Manifest.permission.READ_SMS,
    Manifest.permission.READ_CONTACTS,
    Manifest.permission.CALL_PHONE,
)

fun permissionLabel(p: String): String = when (p) {
    Manifest.permission.READ_CALL_LOG -> "통화기록 읽기"
    Manifest.permission.READ_SMS -> "문자 읽기"
    Manifest.permission.READ_CONTACTS -> "연락처 읽기"
    Manifest.permission.CALL_PHONE -> "전화 걸기"
    else -> p.substringAfterLast('.')
}

/** 기기의 통화기록·문자·연락처를 읽는다. (모두 읽기 전용) */
class CommRepository(private val context: Context) {

    private val resolver get() = context.contentResolver

    fun has(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    /** 번호키 → 이름. 연락처에 '#' 붙여 저장한 번호도 같은 키로 매칭된다. */
    fun loadContacts(): Map<String, String> {
        if (!has(Manifest.permission.READ_CONTACTS)) return emptyMap()
        val result = HashMap<String, String>()
        runCatching {
            resolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER, "display_name"),
                null, null, null
            )?.use { c ->
                val iNum = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val iName = c.getColumnIndex("display_name")
                while (c.moveToNext()) {
                    val k = PhoneUtils.key(c.getString(iNum))
                    val n = c.getString(iName)
                    if (k.isNotEmpty() && !n.isNullOrBlank()) result.putIfAbsent(k, n)
                }
            }
        }
        return result
    }

    fun loadCalls(limit: Int = 1000): List<Event> {
        if (!has(Manifest.permission.READ_CALL_LOG)) return emptyList()
        val out = ArrayList<Event>()
        runCatching {
            resolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf("_id", CallLog.Calls.NUMBER, CallLog.Calls.TYPE, CallLog.Calls.DATE, CallLog.Calls.DURATION),
                null, null, "${CallLog.Calls.DATE} DESC"
            )?.use { c ->
                val iId = c.getColumnIndex("_id")
                val iNum = c.getColumnIndex(CallLog.Calls.NUMBER)
                val iType = c.getColumnIndex(CallLog.Calls.TYPE)
                val iDate = c.getColumnIndex(CallLog.Calls.DATE)
                val iDur = c.getColumnIndex(CallLog.Calls.DURATION)
                while (c.moveToNext() && out.size < limit) {
                    val raw = c.getString(iNum).orEmpty()
                    val dir = when (c.getInt(iType)) {
                        CallLog.Calls.OUTGOING_TYPE -> Direction.OUT
                        CallLog.Calls.MISSED_TYPE -> Direction.MISSED
                        CallLog.Calls.REJECTED_TYPE, CallLog.Calls.BLOCKED_TYPE -> Direction.REJECTED
                        else -> Direction.IN
                    }
                    out += Event(
                        id = "c" + c.getLong(iId),
                        kind = Kind.CALL,
                        direction = dir,
                        line = PhoneUtils.lineOf(raw),
                        key = PhoneUtils.key(raw),
                        rawNumber = raw,
                        time = c.getLong(iDate),
                        durationSec = c.getLong(iDur),
                    )
                }
            }
        }
        return out
    }

    fun loadSms(limit: Int = 2000): List<Event> {
        if (!has(Manifest.permission.READ_SMS)) return emptyList()
        val out = ArrayList<Event>()
        runCatching {
            resolver.query(
                Uri.parse("content://sms"),
                arrayOf("_id", "address", "body", "date", "type"),
                null, null, "date DESC"
            )?.use { c ->
                val iId = c.getColumnIndex("_id")
                val iAddr = c.getColumnIndex("address")
                val iBody = c.getColumnIndex("body")
                val iDate = c.getColumnIndex("date")
                val iType = c.getColumnIndex("type")
                while (c.moveToNext() && out.size < limit) {
                    // 1 받은편지함, 2 보낸편지함, 4 발송중, 5 실패, 6 대기 / 3 임시저장은 제외
                    val dir = when (c.getInt(iType)) {
                        1 -> Direction.IN
                        2, 4, 5, 6 -> Direction.OUT
                        else -> null
                    } ?: continue
                    val raw = c.getString(iAddr).orEmpty()
                    out += Event(
                        id = "s" + c.getLong(iId),
                        kind = Kind.SMS,
                        direction = dir,
                        line = PhoneUtils.lineOf(raw),
                        key = PhoneUtils.key(raw),
                        rawNumber = raw,
                        time = c.getLong(iDate),
                        body = c.getString(iBody),
                    )
                }
            }
        }
        return out
    }
}
