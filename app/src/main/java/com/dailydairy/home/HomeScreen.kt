package com.dailydairy.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.automirrored.outlined.StickyNote2
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dailydairy.Routes
import com.dailydairy.diary.DiaryDatabase
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private fun greetingFor(hour: Int): String = when (hour) {
    in 1..3 -> "Uyku tutmadı mı?"
    in 4..11 -> "Günaydın"
    in 12..16 -> "Merhaba"
    in 17..21 -> "İyi Akşamlar"
    else -> "İyi Geceler"
}

@Composable
fun HomeScreen(
    onOpen: (String) -> Unit,
) {
    val today = remember {
        LocalDate.now().format(
            DateTimeFormatter.ofPattern("d MMMM, EEEE", Locale.forLanguageTag("tr")),
        )
    }
    val greeting = remember { greetingFor(LocalTime.now().hour) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 18.dp)
            .padding(top = 12.dp, bottom = 24.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(
                    text = greeting,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = if (greeting.length > 12) 26.sp else 34.sp,
                    ),
                    maxLines = 1,
                    softWrap = false,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = today,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(
                onClick = { onOpen(Routes.Settings) },
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surface),
            ) {
                Icon(Icons.Outlined.Settings, contentDescription = "Ayarlar")
            }
        }

        Spacer(Modifier.height(18.dp))

        DiaryHero(onClick = { onOpen(Routes.Diary) })

        Spacer(Modifier.height(14.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HomeCard(
                title = "Notlar",
                subtitle = "Cep defteri",
                icon = Icons.AutoMirrored.Outlined.StickyNote2,
                onClick = { onOpen(Routes.Notes) },
                modifier = Modifier.weight(1f),
            )
            HomeCard(
                title = "Listeler",
                subtitle = "Alışveriş, tarif, yapılacak",
                icon = Icons.Outlined.Checklist,
                onClick = { onOpen(Routes.Shopping) },
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HomeCard(
                title = "İzleme listem",
                subtitle = "Film ve dizi",
                icon = Icons.Outlined.Movie,
                onClick = { onOpen(Routes.Watched) },
                modifier = Modifier.weight(1f),
            )
            HomeCard(
                title = "Kitaplar",
                subtitle = "Okudukların",
                icon = Icons.Outlined.AutoStories,
                onClick = { onOpen(Routes.Books) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun DiaryHero(onClick: () -> Unit) {
    val context = LocalContext.current
    val todayCount by remember {
        val zone = ZoneId.systemDefault()
        val start = LocalDate.now().atStartOfDay(zone).toInstant().toEpochMilli()
        val end = LocalDate.now().plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        DiaryDatabase.get(context).diaryDao().countCreatedBetween(start, end)
    }.collectAsStateWithLifecycle(initialValue = 0)
    val line = if (todayCount > 0) {
        "Bir şeyler yazdın. Belki hâlâ anlatacak bir şeyin vardır."
    } else {
        "Henüz bir şey yazmadın. Kısa bir cümle yeter."
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickableCard(onClick = onClick)
            .padding(20.dp)
            .heightIn(min = 168.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.EditNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
            }
            Text(
                text = "Günlük",
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.labelLarge,
            )
        }
        Column {
            Text(
                text = "Bugün",
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = line,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun HomeCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .height(132.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickableCard(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
        Column {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(3.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
