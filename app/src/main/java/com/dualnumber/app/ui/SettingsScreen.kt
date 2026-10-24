package com.dualnumber.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dualnumber.app.data.Config
import com.dualnumber.app.data.Line
import com.dualnumber.app.data.PhoneUtils
import com.dualnumber.app.util.Actions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: UiState,
    onSave: (Config) -> Unit,
    onSetup: () -> Unit,
    onClearAll: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val c = state.config
    var main by rememberSaveable { mutableStateOf(c.mainNumber) }
    var dual by rememberSaveable { mutableStateOf(c.dualNumber) }
    var testInput by rememberSaveable { mutableStateOf("") }
    var clearOpen by remember { mutableStateOf(false) }
    val dualStyle = lineStyle(Line.DUAL)
    val cur = c.copy(mainNumber = main, dualNumber = dual)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("설정", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            SectionCard("전화 구분 설정") {
                StatusRow(
                    title = "발신자 확인 및 스팸 앱",
                    ok = state.setup.roleHeld,
                    okText = "지정됨",
                    badText = if (state.setup.roleAvailable) "필요" else "미지원",
                )
                StatusRow(title = "알림", ok = state.setup.notificationsOk, okText = "허용됨", badText = "꺼짐")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onSetup, shape = RoundedCornerShape(12.dp)) { Text("설정하기") }
                    OutlinedButton(onClick = { Actions.openDefaultApps(context) }, shape = RoundedCornerShape(12.dp)) { Text("기본 앱 설정") }
                }
                Hint("직접 설정: 설정 → 애플리케이션 → 기본 앱 선택 → 발신자 확인 및 스팸 앱 → 듀얼넘버. 기존 스팸 차단 앱(예: 후후, T전화) 대신 지정돼요.")
            }

            SectionCard("내 번호") {
                NumberField("기본번호", main, Line.MAIN) {
                    main = it
                    onSave(c.copy(mainNumber = it, dualNumber = dual))
                }
                NumberField("듀얼번호", dual, Line.DUAL) {
                    dual = it
                    onSave(c.copy(mainNumber = main, dualNumber = it))
                }
                Hint("화면·알림 표시용이에요. 어느 내 번호로 전화가 왔는지 실제 번호로 보여줘요.")
            }

            SectionCard("알림") {
                SwitchRow("듀얼번호로 온 전화 알림", "전화가 울릴 때 바로 알려줘요", c.notifyDual) {
                    onSave(cur.copy(notifyDual = it))
                }
                SwitchRow("기본번호로 온 전화도 알림", "보통은 꺼 두는 걸 권해요", c.notifyMain) {
                    onSave(cur.copy(notifyMain = it))
                }
                TextButton(onClick = { Actions.openNotificationSettings(context) }) { Text("알림 소리·진동 설정") }
            }

            SectionCard("듀얼 발신 기호") {
                Segmented(
                    options = listOf("#  안드로이드", "*  아이폰"),
                    selected = if (c.suffix == '#') 0 else 1,
                    onSelect = { onSave(cur.copy(suffix = if (it == 0) '#' else '*')) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Hint("갤럭시는 # 을 사용하세요. 상대 번호 뒤에 붙여 발신하면 상대에게 내 듀얼번호가 표시돼요.  예) 01099990000${c.suffix}")
            }

            SectionCard("판별 테스트") {
                OutlinedTextField(
                    value = testInput,
                    onValueChange = { testInput = it },
                    label = { Text("번호 입력 (예: 01099990000#)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (testInput.isNotBlank()) {
                    val l = PhoneUtils.lineOf(testInput)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LineBadge(l)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "${lineStyle(l).longLabel}로 온 연락으로 판별돼요",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            SectionCard("동작 방식") {
                Hint(
                    "• 걸려온 번호 끝에 # 또는 * 가 있으면 듀얼, 없으면 기본으로 분류해요\n" +
                        "• 앱 설치·설정 이후의 전화부터 기록돼요\n" +
                        "• 안드로이드 정책상, 연락처에 그대로 저장된 번호는 앱에 전달되지 않을 수 있어요 " +
                        "(듀얼번호로 온 전화는 끝에 # 이 붙어 보통 전달돼요)\n" +
                        "• 전화는 막지 않아요. 번호만 확인해요\n" +
                        "• 전화·문자 회신은 전화 앱·메시지 앱에 번호를 넣어 열어줘요\n" +
                        "• 문자·통화기록·연락처 권한을 쓰지 않아요. 기록은 폰 안에만 저장돼요"
                )
            }

            SectionCard("기록") {
                Text("저장된 통화 ${state.events.size}건", style = MaterialTheme.typography.bodyLarge)
                OutlinedButton(
                    onClick = { clearOpen = true },
                    enabled = state.events.isNotEmpty(),
                    shape = RoundedCornerShape(12.dp),
                ) { Text("전체 기록 삭제", color = MaterialTheme.colorScheme.error) }
            }

            Text(
                "듀얼넘버 라이트 2.0.0",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            )
        }
    }

    if (clearOpen) {
        AlertDialog(
            onDismissRequest = { clearOpen = false },
            title = { Text("전체 기록 삭제") },
            text = { Text("앱에 저장된 통화 기록을 모두 지워요. 휴대폰 통화기록은 그대로예요.") },
            confirmButton = {
                TextButton(onClick = { clearOpen = false; onClearAll() }) {
                    Text("삭제", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { clearOpen = false }) { Text("취소") } },
        )
    }
}

@Composable
private fun StatusRow(title: String, ok: Boolean, okText: String, badText: String) {
    val dual = lineStyle(Line.DUAL)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(
            if (ok) okText else badText,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (ok) dual.accent else MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun SwitchRow(title: String, sub: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val dual = lineStyle(Line.DUAL)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Hint(sub)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = dual.accent),
        )
    }
}

@Composable
private fun Hint(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
