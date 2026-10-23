package com.dualnumber.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dualnumber.app.data.Line

@Composable
fun LineBadge(line: Line, modifier: Modifier = Modifier) {
    val s = lineStyle(line)
    Text(
        text = s.label,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(s.container)
            .padding(horizontal = 7.dp, vertical = 2.dp),
        color = s.onContainer,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
fun LineDot(color: Color, size: Dp = 8.dp) {
    Box(Modifier.size(size).clip(CircleShape).background(color))
}

/** 연락처 이름이 있으면 첫 글자, 없으면 사람 아이콘 */
@Composable
fun Avatar(name: String?, line: Line, size: Dp = 46.dp) {
    val s = lineStyle(line)
    Box(
        modifier = Modifier.size(size).clip(CircleShape).background(s.container),
        contentAlignment = Alignment.Center,
    ) {
        val initial = name?.trim()?.firstOrNull()?.takeIf { !it.isDigit() && it != '+' }
        if (initial != null) {
            Text(
                text = initial.toString(),
                color = s.onContainer,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.4f).sp,
            )
        } else {
            Icon(Icons.Filled.Person, contentDescription = null, tint = s.onContainer, modifier = Modifier.size(size * 0.5f))
        }
    }
}

/** 둥근 세그먼트 선택기 */
@Composable
fun Segmented(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    dots: List<Color?> = emptyList(),
) {
    Row(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp)
    ) {
        options.forEachIndexed { i, label ->
            val isSel = i == selected
            val bg by animateColorAsState(
                if (isSel) MaterialTheme.colorScheme.surfaceContainerLowest else Color.Transparent,
                label = "segment",
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(bg)
                    .clickable { onSelect(i) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    dots.getOrNull(i)?.let {
                        LineDot(it, 7.dp)
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (isSel) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

@Composable
fun NumberField(label: String, value: String, line: Line, onChange: (String) -> Unit) {
    val s = lineStyle(line)
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        placeholder = { Text("010-0000-0000") },
        leadingIcon = { LineDot(s.accent, 10.dp) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = s.accent, focusedLabelColor = s.accent),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
fun ActionButton(
    label: String,
    icon: ImageVector,
    style: LineStyle,
    modifier: Modifier = Modifier,
    filled: Boolean = true,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(50.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (filled) style.accent else style.container,
            contentColor = if (filled) style.onAccent else style.onContainer,
        ),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, fontWeight = FontWeight.SemiBold)
    }
}

/** 앱 로고: 겹친 두 원 */
@Composable
fun DualLogo(logoSize: Dp = 64.dp) {
    val a = lineStyle(Line.MAIN).accent
    val b = lineStyle(Line.DUAL).accent
    Canvas(Modifier.size(logoSize)) {
        val r = this.size.minDimension * 0.32f
        drawCircle(a, r, center = Offset(this.size.width * 0.36f, this.size.height / 2f))
        drawCircle(b.copy(alpha = 0.9f), r, center = Offset(this.size.width * 0.64f, this.size.height / 2f))
    }
}
