package com.voicechanger.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.voicechanger.app.domain.model.VoicePreset
import com.voicechanger.app.presentation.theme.NeonPurple
import com.voicechanger.app.presentation.theme.SurfaceDark
import com.voicechanger.app.presentation.theme.TextPrimary

@Composable
fun VoicePresetSelector(
    selected: VoicePreset,
    onSelect: (VoicePreset) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.size(width = 340.dp, height = 220.dp)
    ) {
        items(VoicePreset.entries) { preset ->
            val isSelected = preset == selected
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (isSelected) NeonPurple.copy(alpha = 0.35f) else SurfaceDark)
                    .clickable { onSelect(preset) }
                    .padding(8.dp)
            ) {
                Text(preset.emoji, style = MaterialTheme.typography.headlineMedium)
                Text(preset.displayName, style = MaterialTheme.typography.labelSmall, color = TextPrimary)
            }
        }
    }
}
