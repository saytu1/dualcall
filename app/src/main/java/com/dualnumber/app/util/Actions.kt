package com.dualnumber.app.util

import android.app.role.RoleManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.provider.Settings
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import com.dualnumber.app.data.Config
import com.dualnumber.app.data.Line
import com.dualnumber.app.data.PhoneUtils

/** 외부 앱 연동 (전화 앱 다이얼러, 메시지 앱, 시스템 설정). 위험 권한 없이 동작. */
object Actions {

    fun dialIntent(number: String) =
        Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(number)))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun smsIntent(number: String, body: String?) =
        Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + Uri.encode(number))).apply {
            if (!body.isNullOrBlank()) putExtra("sms_body", body)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    fun pickPhoneIntent() = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)

    /** 전화 앱을 열고 번호를 입력해 둔다. 통화 버튼만 누르면 된다. */
    fun dial(context: Context, key: String, line: Line, config: Config) {
        if (key.isEmpty()) return toast(context, "번호가 없어 전화를 걸 수 없어요")
        runCatching { context.startActivity(dialIntent(PhoneUtils.dial(key, line, config.suffix))) }
            .onFailure { toast(context, "전화 앱을 열 수 없어요") }
    }

    /** 기본 메시지 앱(삼성 메시지)을 열고 받는 사람 번호를 채운다. */
    fun sms(context: Context, key: String, line: Line, config: Config, body: String? = null) {
        if (key.isEmpty()) return toast(context, "번호가 없어 문자를 보낼 수 없어요")
        runCatching { context.startActivity(smsIntent(PhoneUtils.dial(key, line, config.suffix), body)) }
            .onFailure { toast(context, "메시지 앱을 열 수 없어요") }
    }

    fun copy(context: Context, key: String, line: Line, config: Config) {
        val number = PhoneUtils.dial(key, line, config.suffix)
        context.getSystemService(ClipboardManager::class.java)
            ?.setPrimaryClip(ClipData.newPlainText("번호", number))
        toast(context, "복사했어요: $number")
    }

    fun openDefaultApps(context: Context) {
        runCatching { context.startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)) }
            .onFailure { openAppSettings(context) }
    }

    fun openNotificationSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        runCatching { context.startActivity(intent) }.onFailure { openAppSettings(context) }
    }

    fun openAppSettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null)
        )
        runCatching { context.startActivity(intent) }
    }

    fun toast(context: Context, msg: String) =
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
}

/** 발신자 확인 역할·알림 상태 확인 */
object Setup {
    private fun rm(ctx: Context) = ctx.getSystemService(RoleManager::class.java)

    fun roleAvailable(ctx: Context): Boolean =
        runCatching { rm(ctx)?.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) == true }.getOrDefault(false)

    fun roleHeld(ctx: Context): Boolean =
        runCatching { rm(ctx)?.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) == true }.getOrDefault(false)

    fun roleRequestIntent(ctx: Context): Intent? =
        if (roleAvailable(ctx)) runCatching { rm(ctx)?.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING) }.getOrNull()
        else null

    fun notificationsEnabled(ctx: Context): Boolean =
        NotificationManagerCompat.from(ctx).areNotificationsEnabled()
}
