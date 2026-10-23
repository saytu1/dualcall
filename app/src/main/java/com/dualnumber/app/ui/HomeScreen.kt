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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: UiState,
    onOpen: (Conversation) -> Unit,
    onNew: () -> Unit,
    onSettings: () -> Unit,
    onRequestPermissions: () -> Unit,
) {
    val context = LocalContext.current
    var lineTab by rememberSaveable { mutableIntStateOf(0) } // 0 전체, 1 기본, 2 듀얼
    var kindTab by rememberSaveable { mutableIntStateOf(0) } // 0 전체, 1 전화, 2 문자
    var query by rememberSaveable { mutableStateOf("") }

    val all = remember(state.events, state.contacts) { groupConversations(state.events, state.contacts) }
    val mainCount = remember(all) { all.count { it.line == Line.MAIN } }
    val dualCount = remember(all) { all.count { it.line == Line.DUAL } }

    val list = remember(state.events, state.contacts, lineTab, kindTab, query) {
        val filtered = state.events.filter { e ->
            val kindOk = when (kindTab) {
                1 -> e.kind == Kind.CALL
                2 -> e.kind == Kind.SMS
                else -> true
            }
            val lineOk = when (lineTab) {
                1 -> e.line == Line.MAIN
                2 -> e.line == Line.DUAL
                else -> true
            }
            kindOk && lineOk
        }
        val q = query.trim()
        val qd = q.filter { it.isDigit() }
        groupConversations(filtered, state.contacts).filter { c ->
            q.isEmpty() ||
                c.name?.contains(q, ignoreCase = true) == true ||
                (qd.isNotEmpty() && c.key.contains(qd))
        }
    }

    val missing = REQUIRED_PERMISSIONS.filterNot { it in state.granted }
    val mainStyle = lineStyle(Line.MAIN)
    val dualStyle = lineStyle(Line.DUAL)

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
            if (missing.isNotEmpty() && !state.loading) {
                item {
                    PermissionCard(
                        missing = missing,
                        onRequest = onRequestPermissions,
                        onOpenSettings = { Actions.openAppSettings(context) },
                    )
                }
            }
            item { SearchField(query) { query = it } }
            item {
                Segmented(
                    options = listOf("전체", "기본", "듀얼"),
                    selected = lineTab,
                    onSelect = { lineTab = it },
                    dots = listOf(null, mainStyle.accent, dualStyle.accent),
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                        .fillMaxWidth(),
                )
            }
            item {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf("전체", "전화", "문자").forEachIndexed { i, label ->
                        FilterChip(
                            selected = i == kindTab,
                            onClick = { kindTab = i },
                            label = { Text(label) },
                            shape = RoundedCornerShape(50),
                        )
                    }
                }
            }
            when {
                state.loading -> item {
                    Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(strokeWidth = 3.dp)
                    }
                }
                list.isEmpty() -> item { EmptyState(lineTab) }
                else -> items(list, key = { it.id }) { c ->
                    ConversationRow(c) { onOpen(c) }
                }
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
                "기본·듀얼 번호로 온 연락을 구분해요",
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
        Text("대화 ${count}건", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PermissionCard(missing: List<String>, onRequest: () -> Unit, onOpenSettings: () -> Unit) {
    Column(
        modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("권한이 필요해요", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Text(
            missing.joinToString(" · ") { permissionLabel(it) },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onRequest, shape = RoundedCornerShape(12.dp)) { Text("권한 허용") }
            OutlinedButton(onClick = onOpenSettings, shape = RoundedCornerShape(12.dp)) { Text("설정 열기") }
        }
        Text(
            "허용이 안 되면: 설정 열기 → 오른쪽 위 ⋮ → '제한된 설정 허용' → 권한에서 직접 허용",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
    val missed = e.kind == Kind.CALL && (e.direction == Direction.MISSED || e.direction == Direction.REJECTED)
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
                text = previewText(e),
                style = MaterialTheme.typography.bodyMedium,
                color = if (missed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
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
private fun EmptyState(lineTab: Int) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        DualLogo(48.dp)
        Spacer(Modifier.height(16.dp))
        Text("표시할 기록이 없어요", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(
            text = when (lineTab) {
                2 -> "듀얼번호로 주고받은 전화·문자가 여기에 모여요"
                1 -> "기본번호로 주고받은 전화·문자가 여기에 모여요"
                else -> "통화기록과 문자가 여기에 표시돼요"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
