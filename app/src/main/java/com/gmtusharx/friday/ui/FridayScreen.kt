package com.gmtusharx.friday.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gmtusharx.friday.FridayViewModel
import com.gmtusharx.friday.model.ChatMessage
import com.gmtusharx.friday.model.OrbState
import com.gmtusharx.friday.model.UiState
import com.gmtusharx.friday.ui.components.VoiceOrb
import com.gmtusharx.friday.ui.theme.Midnight
import com.gmtusharx.friday.ui.theme.Navy
import com.gmtusharx.friday.ui.theme.NeonCyan
import com.gmtusharx.friday.ui.theme.NeonPurple
import com.gmtusharx.friday.ui.theme.Panel
import com.gmtusharx.friday.ui.theme.TextMuted
import com.gmtusharx.friday.ui.theme.TextPrimary
import kotlin.math.roundToInt

private data class QuickAction(val label: String, val text: String, val autoSend: Boolean)

private val quickActions = listOf(
    QuickAction("Play YouTube", "YouTube par ", false),
    QuickAction("Search Web", "Search ", false),
    QuickAction("Check Status", "check status", true),
    QuickAction("Open App", "Open ", false),
    QuickAction("Who made you?", "Who made you?", true)
)

@Composable
fun FridayScreen(vm: FridayViewModel) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var input by remember { mutableStateOf("") }
    var showSettings by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) vm.toggleListening() else vm.showPermissionNeeded()
    }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
    LaunchedEffect(ui.messages.size) {
        if (ui.messages.isNotEmpty()) listState.animateScrollToItem(ui.messages.lastIndex)
    }

    val onMic: () -> Unit = {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
            vm.toggleListening()
        else micLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }
    val send: () -> Unit = { if (input.isNotBlank()) { vm.submit(input); input = "" } }

    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Midnight, Navy, Midnight)))
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        // Header
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("FRIDAY", color = NeonCyan, fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = 6.sp)
                Text("by GM TUSHARX", color = TextMuted, fontSize = 12.sp, letterSpacing = 2.sp)
            }
            IconButton(onClick = { showSettings = true }) {
                Icon(Icons.Filled.Settings, "Settings", tint = TextMuted)
            }
        }

        // Orb + status
        Box(Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
            VoiceOrb(ui.orb, ui.level, Modifier.size(220.dp), onClick = onMic)
        }
        Text(
            text = ui.partial.ifBlank {
                when (ui.orb) {
                    OrbState.Idle -> "Tap the orb and speak"
                    OrbState.Listening -> "Listening…"
                    OrbState.Processing -> "Thinking…"
                    OrbState.Speaking -> "Speaking…"
                }
            },
            color = if (ui.partial.isBlank()) TextMuted else TextPrimary,
            textAlign = TextAlign.Center,
            fontSize = 14.sp,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp)
        )

        // Chat history
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(ui.messages, key = { it.id }) { MessageCard(it) }
        }

        // Quick actions
        LazyRow(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(quickActions) { qa ->
                AssistChip(
                    onClick = { if (qa.autoSend) vm.submit(qa.text) else input = qa.text },
                    label = { Text(qa.label) },
                    colors = AssistChipDefaults.assistChipColors(containerColor = Panel, labelColor = NeonCyan),
                    border = AssistChipDefaults.assistChipBorder(enabled = true, borderColor = NeonCyan.copy(alpha = 0.5f))
                )
            }
        }

        // Bottom control bar
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type a command…", color = TextMuted) },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { send() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = NeonCyan.copy(alpha = 0.35f),
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = NeonCyan,
                    focusedContainerColor = Panel,
                    unfocusedContainerColor = Panel
                ),
                trailingIcon = {
                    if (input.isNotBlank()) {
                        IconButton(onClick = send) { Icon(Icons.AutoMirrored.Filled.Send, "Send", tint = NeonCyan) }
                    }
                }
            )
            val listening = ui.orb == OrbState.Listening
            val micScale by animateFloatAsState(if (listening) 1.12f else 1f, label = "micScale")
            Box(
                Modifier
                    .size(56.dp)
                    .scale(micScale)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(NeonCyan, NeonPurple)))
                    .clickable(onClick = onMic),
                contentAlignment = Alignment.Center
            ) {
                Icon(if (listening) Icons.Filled.Stop else Icons.Filled.Mic, "Microphone", tint = Midnight)
            }
        }
    }

    if (showSettings) {
        SettingsDialog(
            ui = ui,
            onDismiss = { showSettings = false },
            onMinutes = vm::setInactivityMinutes,
            onEnabled = vm::setProactiveEnabled,
            onLanguage = vm::setLanguage
        )
    }
}

@Composable
private fun MessageCard(m: ChatMessage) {
    val accent = if (m.fromUser) NeonPurple else NeonCyan
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (m.fromUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (m.fromUser) NeonPurple.copy(alpha = 0.18f) else Panel.copy(alpha = 0.9f),
            border = BorderStroke(1.dp, accent.copy(alpha = 0.5f)),
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Text(m.text, color = TextPrimary, fontSize = 15.sp, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsDialog(
    ui: UiState,
    onDismiss: () -> Unit,
    onMinutes: (Int) -> Unit,
    onEnabled: (Boolean) -> Unit,
    onLanguage: (String) -> Unit
) {
    var minutes by remember { mutableFloatStateOf(ui.inactivityMinutes.toFloat()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Panel,
        title = { Text("Friday settings", color = TextPrimary) },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done", color = NeonCyan) } },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Caring check-ins", color = TextPrimary, modifier = Modifier.weight(1f))
                    Switch(checked = ui.proactiveEnabled, onCheckedChange = onEnabled)
                }
                Text("Check in after ${minutes.roundToInt()} min of silence", color = TextMuted, fontSize = 13.sp)
                Slider(
                    value = minutes,
                    onValueChange = { minutes = it },
                    onValueChangeFinished = { onMinutes(minutes.roundToInt()) },
                    valueRange = 1f..30f,
                    steps = 28,
                    enabled = ui.proactiveEnabled
                )
                Text("Voice language", color = TextMuted, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = ui.language == "hi-IN", onClick = { onLanguage("hi-IN") }, label = { Text("हिन्दी / Hinglish") })
                    FilterChip(selected = ui.language == "en-IN", onClick = { onLanguage("en-IN") }, label = { Text("English") })
                }
            }
        }
    )
}
