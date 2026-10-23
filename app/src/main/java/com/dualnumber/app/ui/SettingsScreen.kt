package com.dualnumber.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dualnumber.app.data.Config
import com.dualnumber.app.data.Line
import com.dualnumber.app.data.REQUIRED_PERMISSIONS
import com.dualnumber.app.data.permissionLabel
import com.dualnumber.app.util.Actions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: UiState,
    onSave: (Config) -> Unit,
    onBack: () -> Unit,
    onRequestPermissions: () -> Unit,
) {
    val context = LocalContext.current
    val c = state.config
    var main by rememberSaveable { mutableStateOf(c.mainNumber) }
    var dual by rememberSaveable { mutableStateOf(c.dualNumber) }
    val dualStyle = lineStyle(Line.DUAL)

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
            SectionCard("내 번호") {
                NumberField("기본번호", main, Line.MAIN) {
                    main = it
                    onSave(c.copy(mainNumber = it, dualNumber = dual))
                }
                NumberField("듀얼번호", dual, Line.DUAL) {
                    dual = it
                    onSave(c.copy(mainNumber = main, dualNumber = it))
                }
                Hint("화면 표시용이에요. 어느 내 번호로 연락이 왔는지 실제 번호로 보여줘요.")
            }

            SectionCard("듀얼 발신 기호") {
                Segmented(
                    options = listOf("#  안드로이드", "*  아이폰"),
                    selected = if (c.suffix == '#') 0 else 1,
                    onSelect = { onSave(c.copy(mainNumber = main, dualNumber = dual, suffix = if (it == 0) '#' else '*')) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Hint("갤럭시는 # 을 사용하세요. 상대 번호 뒤에 붙여 발신하면 상대에게 내 듀얼번호가 표시돼요.  예) 01099990000${c.suffix}")
            }

            SectionCard("전화 걸기") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("바로 걸기", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Hint("끄면 전화 앱 다이얼러에 번호만 입력돼요")
                    }
                    Switch(
                        checked = c.directCall,
                        onCheckedChange = { onSave(c.copy(mainNumber = main, dualNumber = dual, directCall = it)) },
                        colors = SwitchDefaults.colors(checkedTrackColor = dualStyle.accent),
                    )
                }
            }

            SectionCard("권한") {
                REQUIRED_PERMISSIONS.forEach { p ->
                    val ok = p in state.granted
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(permissionLabel(p), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        Text(
                            if (ok) "허용됨" else "필요",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = if (ok) dualStyle.accent else MaterialTheme.colorScheme.error,
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onRequestPermissions, shape = RoundedCornerShape(12.dp)) { Text("권한 다시 요청") }
                    OutlinedButton(onClick = { Actions.openAppSettings(context) }, shape = RoundedCornerShape(12.dp)) { Text("앱 설정 열기") }
                }
                Hint("갤럭시에서 '제한된 설정' 안내가 뜨면: 앱 설정 열기 → 오른쪽 위 ⋮ → '제한된 설정 허용' 후 권한을 직접 켜 주세요.")
            }

            SectionCard("판별 방식") {
                Hint(
                    "• 상대 번호 끝에 # 또는 * 가 붙어 기록된 전화·문자 → 듀얼\n" +
                        "• 기호 없이 기록된 것 → 기본\n" +
                        "• 문자는 통신사에 따라 기호가 붙지 않을 수 있어요\n" +
                        "• 문자 회신은 삼성 메시지 앱에 번호를 넣어 열어줘요"
                )
            }

            Text(
                "듀얼넘버 1.0.0",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            )
        }
    }
}

@Composable
private fun Hint(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
