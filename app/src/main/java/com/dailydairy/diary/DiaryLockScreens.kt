package com.dailydairy.diary

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.dailydairy.ui.PasswordField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiarySetupScreen(onBack: () -> Unit, onCreate: (password: String, hint: String) -> Unit) {
    var password by rememberSaveable { mutableStateOf("") }
    var again by rememberSaveable { mutableStateOf("") }
    var hint by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var confirm by rememberSaveable { mutableStateOf(false) }
    BackHandler(onBack = onBack)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { Text("Günlük kilidi", style = MaterialTheme.typography.headlineMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Text(
                text = "Günlük her açılışta bu şifreyi ister. İpucu, unutursan ekranda görünür. Şifrenin kendisi görünmez.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(18.dp))
            PasswordField(
                value = password,
                onValueChange = { password = it; error = null },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Şifre") },
            )
            Spacer(Modifier.height(12.dp))
            PasswordField(
                value = again,
                onValueChange = { again = it; error = null },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Şifre tekrar") },
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = hint,
                onValueChange = { hint = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("İpucu") },
                singleLine = true,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Şifreyi yazma. Hatırlatacak bir cümle yeter.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            error?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = {
                    error = when {
                        password.length < 4 -> "Şifre en az 4 karakter olsun."
                        password != again -> "İki şifre aynı değil."
                        hint.isBlank() -> "Bir ipucu yaz. Unutunca buna bakacaksın."
                        else -> null
                    }
                    if (error == null) confirm = true
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Kilidi kur") }
        }
    }
    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("Bu şifre geri gelmez") },
            text = {
                Text("Şifreyi ve ipucunu unutursan günlüğü açmanın yolu kalmaz. Yazdıkların da açılmaz. Emin misin?")
            },
            confirmButton = {
                TextButton(onClick = {
                    confirm = false
                    onCreate(password, hint)
                }) { Text("Eminim, kaydet") }
            },
            dismissButton = {
                TextButton(onClick = { confirm = false }) { Text("Geri dön") }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryUnlockScreen(
    hint: String,
    fingerprint: Boolean,
    onBack: () -> Unit,
    matches: (String) -> Boolean,
    onUnlock: () -> Unit,
) {
    var password by rememberSaveable { mutableStateOf("") }
    var showHint by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf(false) }
    val activity = LocalContext.current as FragmentActivity
    BackHandler(onBack = onBack)

    LaunchedEffect(fingerprint) {
        if (fingerprint && Fingerprint.available(activity)) {
            Fingerprint.ask(activity, onSuccess = onUnlock)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { Text("Günlük", style = MaterialTheme.typography.headlineMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
                .imePadding()
                .padding(horizontal = 20.dp),
        ) {
            Text(
                text = "Yazıların kilitli.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(18.dp))
            PasswordField(
                value = password,
                onValueChange = { password = it; error = false },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Şifre") },
                isError = error,
            )
            if (error) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Şifre yanlış.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Spacer(Modifier.height(4.dp))
            TextButton(onClick = { showHint = !showHint }) {
                Text(if (showHint) "İpucunu gizle" else "Şifremi unuttum")
            }
            if (showHint) {
                Text(
                    text = hint.ifBlank { "İpucu kaydedilmemiş." },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    if (matches(password)) onUnlock() else error = true
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = password.isNotEmpty(),
            ) { Text("Aç") }
        }
    }
}
