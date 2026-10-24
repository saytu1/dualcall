package com.dualnumber.app.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.dualnumber.app.MainActivity
import com.dualnumber.app.R
import com.dualnumber.app.data.Config
import com.dualnumber.app.data.Event
import com.dualnumber.app.data.Line
import com.dualnumber.app.data.PhoneUtils
import com.dualnumber.app.data.displayName
import com.dualnumber.app.util.Actions

object Notifier {
    private const val CH_DUAL = "dual_calls"
    private const val CH_MAIN = "main_calls"

    fun ensureChannels(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(
            NotificationChannel(CH_DUAL, "듀얼번호로 온 전화", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "내 듀얼번호로 전화가 오면 알려줘요"
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_MAIN, "기본번호로 온 전화", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "설정에서 켠 경우에만 알려줘요"
            }
        )
    }

    @SuppressLint("MissingPermission")
    fun notifyIncoming(ctx: Context, e: Event, name: String?, config: Config) {
        val isDual = e.line == Line.DUAL
        if (isDual && !config.notifyDual) return
        if (!isDual && !config.notifyMain) return
        val nmc = NotificationManagerCompat.from(ctx)
        if (!nmc.areNotificationsEnabled()) return
        ensureChannels(ctx)

        val who = displayName(e.key, name)
        val myNumber = if (isDual) config.dualNumber else config.mainNumber
        val toWhat = buildString {
            append(if (isDual) "내 듀얼번호" else "내 기본번호")
            if (myNumber.isNotBlank()) append(" ").append(PhoneUtils.format(PhoneUtils.key(myNumber)))
            append("로 걸려왔어요")
        }
        val reqBase = e.key.hashCode()
        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT

        val open = PendingIntent.getActivity(
            ctx, reqBase,
            Intent(ctx, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_KEY, e.key)
                .putExtra(MainActivity.EXTRA_LINE, e.line.name)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            flags,
        )

        val builder = NotificationCompat.Builder(ctx, if (isDual) CH_DUAL else CH_MAIN)
            .setSmallIcon(R.drawable.ic_stat_dual)
            .setColor(if (isDual) 0xFF0C9A6C.toInt() else 0xFF3B5BDB.toInt())
            .setContentTitle(if (isDual) "듀얼 · $who" else "기본 · $who")
            .setContentText(toWhat)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(if (isDual) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(open)

        if (e.key.isNotEmpty()) {
            val dialNumber = PhoneUtils.dial(e.key, e.line, config.suffix)
            val via = if (isDual) "듀얼로" else "기본으로"
            builder.addAction(
                0, "$via 문자",
                PendingIntent.getActivity(ctx, reqBase + 1, Actions.smsIntent(dialNumber, null), flags)
            )
            builder.addAction(
                0, "$via 전화",
                PendingIntent.getActivity(ctx, reqBase + 2, Actions.dialIntent(dialNumber), flags)
            )
        }

        runCatching { nmc.notify(reqBase, builder.build()) }
    }
}
