package com.dualnumber.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dualnumber.app.data.*
import com.dualnumber.app.util.Actions
import com.dualnumber.app.util.TimeFmt

@Composable
fun HomeScreen(
    state: UiState,
    onOpen: (Conversation) -> Unit,
    onNew: () -> Unit,
    onSettings: () -> Unit,
    onSetup: () -> Unit,
) {
    val context = LocalContext.current
    var lineTab by rememberSaveable { mutableIntStateOf(0) } // 0 전체, 1 기본, 2 듀얼
    var query by rememberSaveable { mutableStateOf("") }

    val all = remember(state.events, state.names) { groupConversations(state.events, state.names) }
    val mainCount = remember(all) { all.count { it.line == Line.MAIN } }
    val dualCount = remember(all) { all.count { it.line == Line.DUAL } }

    val list = remember(all, lineTab, query) {
        val q = query.trim()
        val qd = q.filter { it.isDigit() }
        all.filter { c ->
            val lineOk = when (lineTab) {
                1 -> c.line == Line.MAIN
                2 -> c.line == Line.DUAL
                else -> true
            }
            val queryOk = q.isEmpty() ||
                c.name?.contains(q, ignoreCase = true) == true ||
                (qd.isNotEmpty() && c.key.contains(qd))
            lineOk && queryOk
        }
    }

    val setup = state.setup
    val needsSetup = !setup.roleHeld || !setup.notificationsOk

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNew,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(18.dp),
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("새로 보내기", fontWeight = FontWeight.SemiBold) },
            )
        },
    ) { pad ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = pad.calculateTopPadding(),
                bottom = pad.calculateBottomPadding() + 96.dp,
            ),
        ) {
            item { Header(onSettings) }
            item {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    NumberCard(Line.MAIN, state.config.mainNumber, mainCount, lineTab == 1, Modifier.weight(1f)) {
                        lineTab = if (lineTab == 1) 0 else 1
                    }
                    NumberCard(Line.DUAL, state.config.dualNumber, dualCount, lineTab == 2, Modifier.weight(1f)) {
                        lineTab = if (lineTab == 2) 0 else 2
                    }
                }
            }
            if (needsSetup) {
                item {
                    SetupCard(
                        status = setup,
                        onSetup = onSetup,
                        onManual = {
                            if (!setup.roleHeld) Actions.openDefaultApps(context)
                            else Actions.openNotificationSettings(context)
                        },
                    )
                }
            }
            if (all.isNotEmpty()) {
                item { SearchField(query) { query = it } }
            }
            item {
                Segmented(
                    options = listOf("전체", "기본", "듀얼"),
                    selected = lineTab,
                    onSelect = { lineTab = it },
                    dots = listOf(null, lineStyle(Line.MAIN).accent, lineStyle(Line.DUAL).accent),
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                        .fillMaxWidth(),
                )
            }
            if (list.isEmpty()) {
                item { EmptyState(lineTab, filtered = all.isNotEmpty()) }
            } else {
                items(list, key = { it.id }) { c -> ConversationRow(c) { onOpen(c) } }
            }
        }
    }
}

@Composable
private fun Header(onSettings: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("듀얼넘버", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                "기본·듀얼 번호로 온 전화를 구분해요",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onSettings) {
            Icon(Icons.Filled.Settings, contentDescription = "설정")
        }
    }
}

@Composable
private fun NumberCard(
    line: Line,
    number: String,
    count: Int,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val s = lineStyle(line)
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .border(1.5.dp, if (selected) s.accent else Color.Transparent, shape)
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LineDot(s.accent)
            Spacer(Modifier.width(6.dp))
            Text(s.longLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = if (number.isBlank()) "번호 미등록" else PhoneUtils.format(PhoneUtils.key(number)),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = if (number.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(2.dp))
        Text("상대 ${count}명", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SetupCard(status: SetupStatus, onSetup: () -> Unit, onManual: () -> Unit) {
    val dual = lineStyle(Line.DUAL)
    Column(
        modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(dual.container)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            if (!status.roleHeld) "설정을 마무리해 주세요" else "알림을 켜 주세요",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = dual.onContainer,
        )
        Text(
            text = when {
                !status.roleAvailable -> "이 기기는 발신자 확인 앱 지정을 지원하지 않아요. 회신 기능만 사용할 수 있어요."
                !status.roleHeld -> "듀얼넘버를 '발신자 확인 및 스팸 앱'으로 지정해야 걸려오는 전화를 구분할 수 있어요."
                else -> "듀얼번호로 전화가 오면 알림으로 알려드려요."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = dual.onContainer,
        )
        if (status.roleAvailable) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onSetup,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = dual.accent, contentColor = dual.onAccent),
                ) { Text("지금 설정") }
                TextButton(onClick = onManual) { Text("직접 설정", color = dual.onContainer) }
            }
        }
    }
}

@Composable
private fun SearchField(value: String, onChange: (String) -> Unit) {
    Row(
        modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(
                    "이름 또는 번호 검색",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (value.isNotEmpty()) {
            Icon(
                Icons.Filled.Close,
                contentDescription = "지우기",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp).clickable { onChange("") },
            )
        }
    }
}

@Composable
private fun ConversationRow(c: Conversation, onClick: () -> Unit) {
    val e = c.latest
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(c.name, c.line, 48.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = displayName(c.key, c.name),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(Modifier.width(6.dp))
                LineBadge(c.line)
            }
            Spacer(Modifier.height(3.dp))
            Text(
                text = previewText(e) + if (c.events.size > 1) " · ${c.events.size}회" else "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            TimeFmt.short(e.time),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EmptyState(lineTab: Int, filtered: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        DualLogo(48.dp)
        Spacer(Modifier.height(16.dp))
        Text(
            if (filtered) "해당하는 기록이 없어요" else "아직 기록이 없어요",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = when {
                filtered && lineTab == 2 -> "듀얼번호로 주고받은 전화가 여기에 모여요"
                filtered && lineTab == 1 -> "기본번호로 주고받은 전화가 여기에 모여요"
                filtered -> "검색어를 바꿔 보세요"
                else -> "설치 이후 걸려온 전화부터 기록돼요.\n듀얼번호로 전화가 오면 알림으로 알려드려요."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
