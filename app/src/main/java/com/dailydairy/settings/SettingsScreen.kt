package com.dailydairy.settings

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dailydairy.backup.Backup
import com.dailydairy.diary.DiaryViewModel
import com.dailydairy.diary.Fingerprint
import com.dailydairy.diary.barColors
import com.dailydairy.home.ThemeSection
import com.dailydairy.home.clickableCard
import com.dailydairy.theme.ThemeViewModel
import com.dailydairy.ui.PasswordField
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.system.exitProcess

private enum class SettingsPage { Menu, Password, Theme, Backup }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    themeViewModel: ThemeViewModel,
    diaryViewModel: DiaryViewModel = viewModel(),
) {
    var page by rememberSaveable { mutableStateOf(SettingsPage.Menu) }
    val title = when (page) {
        SettingsPage.Menu -> "Ayarlar"
        SettingsPage.Password -> "Günlük şifresi"
        SettingsPage.Theme -> "Tema"
        SettingsPage.Backup -> "Yedek"
    }
    val back = {
        if (page == SettingsPage.Menu) onBack() else page = SettingsPage.Menu
    }
    BackHandler(onBack = back)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(title, style = MaterialTheme.typography.headlineMedium) },
                navigationIcon = {
                    IconButton(onClick = back) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Geri")
                    }
                },
                colors = barColors(),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            Spacer(Modifier.height(4.dp))
            when (page) {
                SettingsPage.Menu -> {
                    SettingsRow(
                        title = "Günlük şifresi",
                        subtitle = "Şifre, ipucu ve parmak izi",
                        icon = Icons.Outlined.Lock,
                        onClick = { page = SettingsPage.Password },
                    )
                    Spacer(Modifier.height(12.dp))
                    SettingsRow(
                        title = "Tema",
                        subtitle = "Açık, karanlık ve palet",
                        icon = Icons.Outlined.Palette,
                        onClick = { page = SettingsPage.Theme },
                    )
                    Spacer(Modifier.height(12.dp))
                    SettingsRow(
                        title = "Yedek",
                        subtitle = "Dışa aktar veya içe al",
                        icon = Icons.Outlined.SaveAlt,
                        onClick = { page = SettingsPage.Backup },
                    )
                }
                SettingsPage.Password -> PasswordSection(diaryViewModel)
                SettingsPage.Theme -> ThemeSection(themeViewModel)
                SettingsPage.Backup -> BackupSection()
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun SettingsRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickableCard(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(8.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PasswordSection(diaryViewModel: DiaryViewModel) {
    val lock by diaryViewModel.lock.collectAsStateWithLifecycle()
    val fingerprintOn by diaryViewModel.fingerprintEnabled.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as FragmentActivity
    val fingerprintReady = Fingerprint.available(context)
    var current by rememberSaveable { mutableStateOf("") }
    var next by rememberSaveable { mutableStateOf("") }
    var again by rememberSaveable { mutableStateOf("") }
    var hint by rememberSaveable(lock?.hint) { mutableStateOf(lock?.hint.orEmpty()) }
    var message by rememberSaveable { mutableStateOf<String?>(null) }
    var failed by rememberSaveable { mutableStateOf(false) }
    var unlocked by rememberSaveable { mutableStateOf(false) }
    var nudge by remember { mutableStateOf(false) }
    var confirm by rememberSaveable { mutableStateOf(false) }
    val currentFocus = remember { FocusRequester() }

    LaunchedEffect(current) {
        if (current.isEmpty()) {
            unlocked = false
            return@LaunchedEffect
        }
        delay(250)
        unlocked = withContext(Dispatchers.Default) { diaryViewModel.passwordMatches(current) }
    }

    if (lock == null) {
        Text(
            text = "Şifre henüz yok. Günlük ilk açıldığında kurulur.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    PasswordField(
        value = current,
        onValueChange = { current = it; message = null; failed = false; nudge = false },
        label = { Text("Mevcut şifre") },
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(currentFocus),
        isError = failed,
    )
    Spacer(Modifier.height(12.dp))
    PasswordField(
        value = next,
        onValueChange = { next = it; message = null },
        label = { Text("Yeni şifre") },
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(12.dp))
    PasswordField(
        value = again,
        onValueChange = { again = it; message = null },
        label = { Text("Yeni şifre tekrar") },
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(12.dp))
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = hint,
            onValueChange = { hint = it; message = null },
            modifier = Modifier.fillMaxWidth(),
            enabled = unlocked,
            label = { Text("Yeni ipucu") },
            singleLine = true,
        )
        if (!unlocked) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clickableCard(onClick = {
                        currentFocus.requestFocus()
                        nudge = true
                    }),
            )
        }
    }
    if (nudge) {
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Önce mevcut şifreyi yaz.",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelMedium,
        )
    }
    Spacer(Modifier.height(16.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "Parmak iziyle aç", style = MaterialTheme.typography.titleMedium)
            Text(
                text = when {
                    !fingerprintReady -> "Telefonda parmak izi yok."
                    fingerprintOn -> "Bu telefonda şifre yazmadan açılır."
                    else -> "Açmak için önce mevcut şifreyi yaz."
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = fingerprintOn && fingerprintReady,
            enabled = fingerprintReady && (fingerprintOn || unlocked),
            onCheckedChange = { want ->
                if (!want) {
                    diaryViewModel.setFingerprint(false)
                    return@Switch
                }
                if (!unlocked) return@Switch
                Fingerprint.ask(
                    activity,
                    onSuccess = { diaryViewModel.setFingerprint(true) },
                )
            },
        )
    }
    Spacer(Modifier.height(8.dp))
    message?.let {
        Text(
            text = it,
            color = if (failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(8.dp))
    }
    Spacer(Modifier.height(8.dp))
    Button(
        onClick = {
            val problem = when {
                current.isEmpty() -> "Mevcut şifreyi yaz."
                !unlocked -> "Mevcut şifre yanlış."
                next.isNotEmpty() && next.length < 4 -> "Yeni şifre en az 4 karakter olsun."
                next.isNotEmpty() && next == current -> "Yeni şifre eskisiyle aynı. Farklı bir şifre seç."
                next != again -> "Yeni şifreler aynı değil."
                hint.isBlank() -> "İpucu boş kalmasın."
                next.isEmpty() && hint == lock?.hint -> "Değişen bir şey yok."
                else -> null
            }
            if (problem != null) {
                message = problem
                failed = problem.startsWith("Mevcut") || problem.startsWith("Yeni şifre eskisi")
                return@Button
            }
            if (next.isNotEmpty()) {
                confirm = true
                return@Button
            }
            diaryViewModel.changePassword(current, next, hint) { ok ->
                if (ok) {
                    current = ""
                    next = ""
                    again = ""
                    failed = false
                    message = "İpucu değişti."
                } else {
                    failed = true
                    message = "Mevcut şifre yanlış."
                }
            }
        },
        modifier = Modifier.fillMaxWidth(),
    ) { Text("Şifreyi değiştir") }
    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("Bu şifre geri gelmez") },
            text = {
                Text("Yeni şifreyi ve ipucunu unutursan günlüğü açmanın yolu kalmaz. Yazdıkların da açılmaz. Emin misin?")
            },
            confirmButton = {
                TextButton(onClick = {
                    confirm = false
                    val chosen = next
                    diaryViewModel.changePassword(current, chosen, hint) { ok ->
                        if (ok) {
                            current = ""
                            next = ""
                            again = ""
                            failed = false
                            message = "Şifre değişti."
                        } else {
                            failed = true
                            message = "Mevcut şifre yanlış."
                        }
                    }
                }) { Text("Eminim, kaydet") }
            },
            dismissButton = {
                TextButton(onClick = { confirm = false }) { Text("Geri dön") }
            },
        )
    }
}

@Composable
private fun BackupSection() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var message by remember { mutableStateOf<String?>(null) }
    var failed by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf<Uri?>(null) }
    var busy by remember { mutableStateOf(false) }

    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            busy = true
            val error = withContext(Dispatchers.IO) { runCatching { Backup.export(context, uri) }.exceptionOrNull() }
            busy = false
            failed = error != null
            message = if (error == null) "Yedek kaydedildi." else "Yedek yazılamadı."
        }
    }
    val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        pending = uri
    }

    Text(
        text = "Günlük, notlar, listeler, filmler, kitaplar, fotoğraflar, tema ve günlük şifresi tek dosyada.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(16.dp))
    Button(
        onClick = { export.launch("dailydairy-yedek.zip") },
        enabled = !busy,
        modifier = Modifier.fillMaxWidth(),
    ) { Text("Dışa aktar") }
    Spacer(Modifier.height(10.dp))
    Button(
        onClick = { import.launch(arrayOf("application/zip", "application/octet-stream")) },
        enabled = !busy,
        modifier = Modifier.fillMaxWidth(),
    ) { Text("İçe al") }
    Spacer(Modifier.height(10.dp))
    Text(
        text = "İçe almak bu telefondakinin üstüne yazar. Geri alınamaz.",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    message?.let {
        Text(
            text = it,
            color = if (failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
    if (pending != null) {
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text("Yedek içe alınsın mı?") },
            text = { Text("Bu telefondaki her şeyin yerine geçer.") },
            confirmButton = {
                TextButton(onClick = {
                    val uri = pending ?: return@TextButton
                    pending = null
                    scope.launch {
                        busy = true
                        val error = withContext(Dispatchers.IO) {
                            runCatching { Backup.restore(context, uri) }.exceptionOrNull()
                        }
                        if (error == null) {
                            exitProcess(0)
                        } else {
                            busy = false
                            failed = true
                            message = "Bu dosya yedek değil."
                        }
                    }
                }) { Text("İçe al") }
            },
            dismissButton = {
                TextButton(onClick = { pending = null }) { Text("Vazgeç") }
            },
        )
    }
}
