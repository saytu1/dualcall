package com.dualnumber.app.ui

import android.app.Activity
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dualnumber.app.data.Line
import com.dualnumber.app.data.PhoneUtils
import com.dualnumber.app.data.displayName
import com.dualnumber.app.util.Actions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewMessageScreen(state: UiState, onSaveName: (String, String?) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    var line by rememberSaveable { mutableStateOf(Line.DUAL) }
    var input by rememberSaveable { mutableStateOf("") }
    val s = lineStyle(line)

    // 시스템 연락처 선택 창: 고른 1건만 임시로 읽을 수 있어 연락처 권한이 필요 없다.
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val uri: Uri = result.data?.data ?: return@rememberLauncherForActivityResult
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME),
                null, null, null,
            )?.use { c ->
                if (c.moveToFirst()) {
                    val k = PhoneUtils.key(c.getString(0))
                    val n = c.getString(1)
                    if (k.isNotEmpty()) {
                        input = PhoneUtils.format(k)
                        if (!n.isNullOrBlank()) onSaveName(k, n)
                    }
                }
            }
        }.onFailure { Actions.toast(context, "연락처를 불러오지 못했어요") }
    }

    val target = PhoneUtils.key(input).takeIf { it.length >= 3 }.orEmpty()
    val recents = remember(state.events, state.names) {
        (state.events.map { it.key } + state.names.keys).filter { it.isNotEmpty() }.distinct().take(50)
    }
    val q = input.filter { it.isDigit() }
    val shown = remember(recents, q) { if (q.isEmpty()) recents else recents.filter { it.contains(q) } }
    val myNumber = if (line == Line.DUAL) state.config.dualNumber else state.config.mainNumber

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("새로 보내기", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.background) {
                Column(
                    Modifier
                        .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
                        .padding(16.dp)
                ) {
                    if (target.isNotEmpty()) {
                        Text(
                            "실제 발신  ${PhoneUtils.dial(target, line, state.config.suffix)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ActionButton("${s.via} 전화", Icons.Filled.Call, s, Modifier.weight(1f), enabled = target.isNotEmpty()) {
                            Actions.dial(context, target, line, state.config)
                        }
                        ActionButton("${s.via} 문자", Icons.Filled.Email, s, Modifier.weight(1f), filled = false, enabled = target.isNotEmpty()) {
                            Actions.sms(context, target, line, state.config)
                        }
                    }
                }
            }
        },
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Text(
                    "어느 번호로 보낼까요?",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Segmented(
                    options = listOf("기본번호로", "듀얼번호로"),
                    selected = if (line == Line.MAIN) 0 else 1,
                    onSelect = { line = if (it == 0) Line.MAIN else Line.DUAL },
                    dots = listOf(lineStyle(Line.MAIN).accent, lineStyle(Line.DUAL).accent),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (myNumber.isBlank()) "상대에게 내 ${s.longLabel}가 표시돼요"
                    else "상대에게 ${PhoneUtils.format(PhoneUtils.key(myNumber))} 로 표시돼요",
                    style = MaterialTheme.typography.bodySmall,
                    color = s.accent,
                    modifier = Modifier.padding(start = 4.dp),
                )
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text("받는 사람 번호") },
                    placeholder = { Text("010-0000-0000") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = s.accent, focusedLabelColor = s.accent),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = {
                        runCatching { picker.launch(Actions.pickPhoneIntent()) }
                            .onFailure { Actions.toast(context, "연락처 앱을 열 수 없어요") }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.AccountCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("연락처에서 선택")
                }
                if (shown.isNotEmpty()) {
                    Text(
                        "최근 · 이름 붙인 번호",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 18.dp, bottom = 4.dp),
                    )
                }
            }
            LazyColumn(Modifier.weight(1f)) {
                items(shown, key = { it }) { key ->
                    val name = state.names[key]
                    val selected = target == key
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { input = PhoneUtils.format(key) }
                            .background(if (selected) s.container else MaterialTheme.colorScheme.background)
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Avatar(name, line, 40.dp)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(displayName(key, name), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            if (name != null) {
                                Text(
                                    PhoneUtils.format(key),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
