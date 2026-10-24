package com.dualnumber.app.service

import android.telecom.Call
import android.telecom.CallScreeningService
import com.dualnumber.app.data.Direction
import com.dualnumber.app.data.Event
import com.dualnumber.app.data.EventStore
import com.dualnumber.app.data.NameStore
import com.dualnumber.app.data.PhoneUtils
import com.dualnumber.app.data.Prefs

/**
 * "발신자 확인 및 스팸 앱" 역할로 동작.
 * 전화를 막지 않고 번호만 확인해서, 끝에 # / * 가 있으면 듀얼로 기록·알림한다.
 * (안드로이드 정책상 연락처에 저장된 번호와 정확히 일치하는 전화는 전달되지 않을 수 있음)
 */
class DualScreeningService : CallScreeningService() {

    override fun onScreenCall(details: Call.Details) {
        val outgoing = details.callDirection == Call.Details.DIRECTION_OUTGOING
        if (!outgoing) {
            // 항상 허용 (차단·무음 처리 없음). 5초 안에 응답해야 하므로 가장 먼저 처리.
            runCatching { respondToCall(details, CallResponse.Builder().build()) }
        }

        runCatching {
            val app = applicationContext
            val raw = details.handle?.schemeSpecificPart.orEmpty()
            val now = System.currentTimeMillis()
            val event = Event(
                id = "e$now-${raw.hashCode()}",
                direction = if (outgoing) Direction.OUT else Direction.IN,
                line = PhoneUtils.lineOf(raw),
                key = PhoneUtils.key(raw),
                rawNumber = raw,
                time = now,
            )
            EventStore.add(app, event)
            if (!outgoing) {
                NameStore.init(app)
                Notifier.notifyIncoming(app, event, NameStore.names.value[event.key], Prefs(app).load())
            }
        }
    }
}
