package com.dailydairy.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dailydairy.theme.Appearance
import com.dailydairy.theme.Palette
import com.dailydairy.theme.ThemeViewModel

@Composable
fun ThemeSection(themeViewModel: ThemeViewModel) {
    val palette by themeViewModel.palette.collectAsStateWithLifecycle()
    val appearance by themeViewModel.appearance.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Görünüm",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.sp,
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Appearance.entries.forEach { item ->
                ChoiceChip(
                    label = item.label,
                    selected = item == appearance,
                    onClick = { themeViewModel.setAppearance(item) },
                )
            }
        }

        Spacer(Modifier.height(22.dp))

        Text(
            text = "Palet",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.sp,
        )
        Spacer(Modifier.height(10.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Palette.entries.forEach { item ->
                ChoiceChip(
                    label = item.label,
                    selected = item == palette,
                    dot = paletteDot(item),
                    onClick = { themeViewModel.setPalette(item) },
                )
            }
        }
    }
}

@Composable
internal fun ChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    dot: Color? = null,
) {
    val bg = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.background
    val fg = if (selected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
            .clickableCard(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (dot != null) {
            Spacer(
                Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(dot),
            )
        }
        Text(text = label, color = fg, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun paletteDot(palette: Palette): Color = when (palette) {
    Palette.Sistem -> MaterialTheme.colorScheme.primary
    Palette.Kagit -> Color(0xFFC4654A)
    Palette.Deniz -> Color(0xFF0E7490)
    Palette.Zeytin -> Color(0xFF5C6B32)
    Palette.Mercan -> Color(0xFFB94B55)
    Palette.Gece -> Color(0xFF5B4DB8)
    Palette.Gul -> Color(0xFFC4527A)
    Palette.Bal -> Color(0xFFA67C1A)
    Palette.Toprak -> Color(0xFF8C5A3C)
}
