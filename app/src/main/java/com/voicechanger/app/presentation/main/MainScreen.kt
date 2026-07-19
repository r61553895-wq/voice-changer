package com.voicechanger.app.presentation.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.voicechanger.app.presentation.components.EffectSlider
import com.voicechanger.app.presentation.components.VoicePresetSelector
import com.voicechanger.app.presentation.theme.BgDark
import com.voicechanger.app.presentation.theme.NeonCyan
import com.voicechanger.app.presentation.theme.NeonPink
import com.voicechanger.app.presentation.theme.NeonPurple
import com.voicechanger.app.presentation.theme.TextSecondary

@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel(),
    hasMicPermission: Boolean,
    onRequestMicPermission: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    Scaffold(containerColor = BgDark) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Voice Changer",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                if (state.isRunning) "Задержка: ~${state.measuredLatencyMs.toInt()} мс" else "Движок остановлен",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Spacer(Modifier.height(24.dp))

            MicButton(
                isRunning = state.isRunning,
                onClick = {
                    if (!hasMicPermission) onRequestMicPermission() else viewModel.toggleEngine()
                }
            )

            state.errorMessage?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = NeonPink, style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.height(28.dp))
            Text("Пресеты", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(10.dp))
            VoicePresetSelector(selected = state.activePreset, onSelect = viewModel::selectPreset)

            Spacer(Modifier.height(24.dp))
            Text("Тонкая настройка", style = MaterialTheme.typography.titleMedium)
            Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                EffectSlider(
                    label = "Pitch",
                    value = state.effectParams.pitchSemitones,
                    range = -12f..12f,
                    onValueChange = viewModel::updatePitch,
                    valueFormatter = { "${it.toInt()} st" }
                )
                EffectSlider(
                    label = "Voice depth",
                    value = state.effectParams.voiceDepth,
                    range = -1f..1f,
                    onValueChange = viewModel::updateVoiceDepth
                )
                EffectSlider(
                    label = "Reverb",
                    value = state.effectParams.reverbMix,
                    range = 0f..1f,
                    onValueChange = viewModel::updateReverb
                )
                EffectSlider(
                    label = "Echo",
                    value = state.effectParams.echoMix,
                    range = 0f..1f,
                    onValueChange = viewModel::updateEcho
                )
            }

            Spacer(Modifier.height(16.dp))
            Box(
                Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.fillMaxWidth()) {
                    Text("AI Voice Conversion", style = MaterialTheme.typography.titleMedium)
                    androidx.compose.foundation.layout.Row(
                        Modifier.fillMaxWidth().padding(top = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (state.aiModelEnabled) "Активна модель: ${state.aiModelName}" else "Test mode (только DSP)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(checked = state.aiModelEnabled, onCheckedChange = viewModel::toggleAiModel)
                    }
                }
            }
        }
    }
}

@Composable
private fun MicButton(isRunning: Boolean, onClick: () -> Unit) {
    val gradient = Brush.radialGradient(listOf(NeonPurple, NeonCyan))
    Box(
        modifier = Modifier
            .size(120.dp)
            .clip(CircleShape)
            .background(gradient),
        contentAlignment = Alignment.Center
    ) {
        Button(
            onClick = onClick,
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = BgDark),
            modifier = Modifier.size(96.dp)
        ) {
            Icon(
                imageVector = if (isRunning) Icons.Filled.Mic else Icons.Filled.MicOff,
                contentDescription = "Toggle voice changer",
                tint = if (isRunning) NeonCyan else TextSecondary,
                modifier = Modifier.size(40.dp)
            )
        }
    }
}
