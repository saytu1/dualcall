package com.dualnumber.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dualnumber.app.data.*
import com.dualnumber.app.util.Actions
import com.dualnumber.app.util.TimeFmt

private sealed interface TimelineRow {
    val id: String

    data class Day(override val id: String, val label: String) : TimelineRow
    data class Item(val event: Event) : TimelineRow {
        override val id: String get() = event.id
    }
}

/** 최신순 목록 + 날짜 구분선 (reverseLayout 이므로 날짜는 그날 가장 오래된 기록 뒤에 넣는다) */
private fun buildTimeline(events: List<Event>): List<TimelineRow> {
    val out = ArrayList<TimelineRow>(events.size + 8)
    events.forEachIndexed { i, e ->
        out += TimelineRow.Item(e)
        val next = events.getOrNull(i + 1)
        if (next == null || !TimeFmt.sameDay(e.time, next.time)) {
            out += TimelineRow.Day("day-" + TimeFmt.dayKey(e.time), TimeFmt.dayHeader(e.time))
        }
    }
    return out
}

@Composable
fun DetailScreen(state: UiState, key: String, line: Line, onBack: () -> Unit) {
    val context = LocalContext.current
    val config = state.config
    val events = remember(state.events, key, line) { state.events.filter { it.key == key && it.line == line } }
    val rows = remember(events) { buildTimeline(events) }
    val name = state.contacts[key]
    val s = lineStyle(line)
    val other = if (line == Line.DUAL) Line.MAIN else Line.DUAL
    val otherStyle = lineStyle(other)
    val myNumber = if (line == Line.DUAL) config.dualNumber else config.mainNumber
    var text by rememberSaveable { mutableStateOf("") }
    var menuOpen by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(
                Modifier
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                    Avatar(name, line, 40.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = displayName(key, name),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            Spacer(Modifier.width(6.dp))
                            LineBadge(line)
                        }
                        if (name != null) {
                            Text(
                                PhoneUtils.format(key),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "더보기")
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("${otherStyle.longLabel}로 전화") },
                                leadingIcon = { LineDot(otherStyle.accent) },
                                onClick = { menuOpen = false; Actions.call(context, key, other, config) },
                            )
                            DropdownMenuItem(
                                text = { Text("${otherStyle.longLabel}로 문자") },
                                leadingIcon = { LineDot(otherStyle.accent) },
                                onClick = { menuOpen = false; Actions.sms(context, key, other, config) },
                            )
                            DropdownMenuItem(
                                text = { Text("발신 번호 복사 (${PhoneUtils.dial(key, line, config.suffix)})") },
                                onClick = { menuOpen = false; Actions.copy(context, key, line, config) },
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ActionButton("${s.via} 전화", Icons.Filled.Call, s, Modifier.weight(1f)) {
                        Actions.call(context, key, line, config)
                    }
                    ActionButton("${s.via} 문자", Icons.Filled.Email, s, Modifier.weight(1f), filled = false) {
                        Actions.sms(context, key, line, config)
                    }
                }

                Text(
                    text = buildString {
                        append("상대에게 보이는 내 번호  ")
                        append(if (myNumber.isBlank()) "${s.longLabel} (설정에서 등록)" else PhoneUtils.format(PhoneUtils.key(myNumber)))
                        append("\n발신 형식  ")
                        append(PhoneUtils.dial(key, line, config.suffix).ifEmpty { "-" })
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        },
        bottomBar = {
            Composer(
                text = text,
                onChange = { text = it },
                style = s,
                onSend = {
                    Actions.sms(context, key, line, config, text)
                    text = ""
                },
            )
        },
    ) { pad ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(pad),
            reverseLayout = true,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(rows, key = { it.id }) { row ->
                when (row) {
                    is TimelineRow.Day -> DayHeader(row.label)
                    is TimelineRow.Item ->
                        if (row.event.kind == Kind.SMS) SmsBubble(row.event, s) else CallPill(row.event)
                }
            }
        }
    }
}

@Composable
private fun DayHeader(label: String) {
    Box(Modifier.fillMaxWidth().padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SmsBubble(e: Event, s: LineStyle) {
    val out = e.direction == Direction.OUT
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (out) Arrangement.End else Arrangement.Start,
    ) {
        Column(horizontalAlignment = if (out) Alignment.End else Alignment.Start) {
            Text(
                text = e.body.orEmpty(),
                style = MaterialTheme.typography.bodyLarge,
                color = if (out) s.onAccent else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .widthIn(max = 290.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 18.dp,
                            bottomStart = if (out) 18.dp else 6.dp,
                            bottomEnd = if (out) 6.dp else 18.dp,
                        )
                    )
                    .background(if (out) s.accent else MaterialTheme.colorScheme.surfaceContainerLowest)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            )
            Text(
                TimeFmt.clock(e.time),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
    }
}

@Composable
private fun CallPill(e: Event) {
    val missed = e.direction == Direction.MISSED || e.direction == Direction.REJECTED
    val color = if (missed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
    Box(Modifier.fillMaxWidth().padding(vertical = 2.dp), contentAlignment = Alignment.Center) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Call, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                "${callLabel(e)} · ${TimeFmt.clock(e.time)}",
                style = MaterialTheme.typography.labelMedium,
                color = color,
            )
        }
    }
}

@Composable
private fun Composer(text: String, onChange: (String) -> Unit, style: LineStyle, onSend: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(Modifier.windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(22.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    if (text.isEmpty()) {
                        Text(
                            "${style.via} 보낼 문자",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    BasicTextField(
                        value = text,
                        onValueChange = onChange,
                        maxLines = 5,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Spacer(Modifier.width(8.dp))
                FilledIconButton(
                    onClick = onSend,
                    enabled = text.isNotBlank(),
                    modifier = Modifier.size(46.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = style.accent,
                        contentColor = style.onAccent,
                    ),
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "보내기")
                }
            }
            Text(
                "메시지 앱이 열리면 전송 버튼을 눌러 주세요",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 24.dp, bottom = 6.dp),
            )
        }
    }
}
