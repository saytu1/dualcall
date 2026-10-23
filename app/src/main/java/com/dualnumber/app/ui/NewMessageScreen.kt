package com.dualnumber.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dualnumber.app.data.Line
import com.dualnumber.app.data.PhoneUtils
import com.dualnumber.app.util.Actions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewMessageScreen(state: UiState, onBack: () -> Unit) {
    val context = LocalContext.current
    var line by rememberSaveable { mutableStateOf(Line.DUAL) }
    var input by rememberSaveable { mutableStateOf("") }
    val s = lineStyle(line)
    val mainStyle = lineStyle(Line.MAIN)
    val dualStyle = lineStyle(Line.DUAL)

    val contacts = remember(state.contacts) {
        state.contacts.entries.map { it.value to it.key }.sortedBy { it.first }
    }
    val q = input.trim()
    val qd = q.filter { it.isDigit() }
    val looksLikeNumber = qd.length >= 3 && q.all { it.isDigit() || it in "-+ ()#*" }
    val target = if (looksLikeNumber) PhoneUtils.key(q) else ""
    val shown = remember(contacts, q) {
        if (q.isEmpty()) contacts
        else contacts.filter { (name, key) ->
            name.contains(q, ignoreCase = true) || (qd.isNotEmpty() && key.contains(qd))
        }
    }
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
                            Actions.call(context, target, line, state.config)
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
                    dots = listOf(mainStyle.accent, dualStyle.accent),
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
                    label = { Text("이름 검색 또는 번호 입력") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = s.accent, focusedLabelColor = s.accent),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
            }
            LazyColumn(Modifier.weight(1f)) {
                items(shown, key = { it.second }) { (name, key) ->
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
                            Text(name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(
                                PhoneUtils.format(key),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                if (shown.isEmpty()) {
                    item {
                        Text(
                            if (looksLikeNumber) "아래 버튼으로 바로 보낼 수 있어요" else "일치하는 연락처가 없어요",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(24.dp).clip(RoundedCornerShape(8.dp)),
                        )
                    }
                }
            }
        }
    }
}
