package com.dualnumber.app.util

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.dualnumber.app.data.Config
import com.dualnumber.app.data.Line
import com.dualnumber.app.data.PhoneUtils

/** 전화 걸기·문자 앱 열기 등 외부 동작 */
object Actions {

    fun call(context: Context, key: String, line: Line, config: Config) {
        if (key.isEmpty()) {
            toast(context, "번호가 없어 전화를 걸 수 없어요")
            return
        }
        val number = PhoneUtils.dial(key, line, config.suffix)
        val uri = Uri.parse("tel:" + Uri.encode(number))
        val canCallDirect = config.directCall &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) ==
            PackageManager.PERMISSION_GRANTED
        val intent = Intent(if (canCallDirect) Intent.ACTION_CALL else Intent.ACTION_DIAL, uri)
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                context.startActivity(Intent(Intent.ACTION_DIAL, uri))
            } catch (e2: Exception) {
                toast(context, "전화 앱을 열 수 없어요")
            }
        }
    }

    /** 기본 메시지 앱(삼성 메시지)을 열고 받는 사람 번호를 채운다. */
    fun sms(context: Context, key: String, line: Line, config: Config, body: String? = null) {
        if (key.isEmpty()) {
            toast(context, "번호가 없어 문자를 보낼 수 없어요")
            return
        }
        val number = PhoneUtils.dial(key, line, config.suffix)
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + Uri.encode(number)))
        if (!body.isNullOrBlank()) intent.putExtra("sms_body", body)
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            toast(context, "메시지 앱을 열 수 없어요")
        }
    }

    fun copy(context: Context, key: String, line: Line, config: Config) {
        val number = PhoneUtils.dial(key, line, config.suffix)
        val cm = context.getSystemService(ClipboardManager::class.java)
        cm?.setPrimaryClip(ClipData.newPlainText("번호", number))
        toast(context, "복사했어요: $number")
    }

    fun openAppSettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null)
        )
        runCatching { context.startActivity(intent) }
    }

    private fun toast(context: Context, msg: String) =
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
}
