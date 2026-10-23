package com.dualnumber.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dualnumber.app.data.Config
import com.dualnumber.app.data.Line

@Composable
fun OnboardingScreen(initial: Config, onDone: (Config) -> Unit) {
    var main by rememberSaveable { mutableStateOf(initial.mainNumber) }
    var dual by rememberSaveable { mutableStateOf(initial.dualNumber) }
    val dualStyle = lineStyle(Line.DUAL)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Box(
                Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
                    .padding(20.dp)
            ) {
                Button(
                    onClick = { onDone(initial.copy(mainNumber = main, dualNumber = dual)) },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text("시작하기", fontWeight = FontWeight.SemiBold)
                }
            }
        },
    ) { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
        ) {
            Spacer(Modifier.height(32.dp))
            DualLogo(64.dp)
            Spacer(Modifier.height(20.dp))
            Text(
                "두 개의 번호,\n한눈에 구분하세요",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "기본번호와 듀얼번호로 온 전화·문자를 나눠 보여주고, 듀얼번호로 바로 회신할 수 있어요.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))

            SectionCard("이렇게 동작해요") {
                Step(1, "상대가 내 듀얼번호로 연락하면 상대 번호 끝에 # 이 붙어 기록돼요", dualStyle)
                Step(2, "앱이 # 표시를 찾아 듀얼로 분류해요", dualStyle)
                Step(3, "회신할 때 상대 번호 뒤에 # 을 자동으로 붙여, 상대에게 내 듀얼번호로 보여요", dualStyle)
            }

            Spacer(Modifier.height(20.dp))
            SectionCard("내 번호 등록 (선택)") {
                NumberField("기본번호", main, Line.MAIN) { main = it }
                NumberField("듀얼번호", dual, Line.DUAL) { dual = it }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "시작하면 통화기록 · 문자 · 연락처 · 전화 권한을 요청해요. 기록은 휴대폰 밖으로 나가지 않아요.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Step(n: Int, text: String, style: LineStyle) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier.size(24.dp).clip(CircleShape).background(style.container),
            contentAlignment = Alignment.Center,
        ) {
            Text("$n", style = MaterialTheme.typography.labelMedium, color = style.onContainer, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 2.dp))
    }
}
