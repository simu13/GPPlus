package com.example.gpplus.ui.theme

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gpplus.VoiceState

// ----------------------------------------------------------
//                BOTTOM MIC BAR WITH TRANSCRIPT
// ----------------------------------------------------------
@Composable
fun BottomMicBar(
    voiceState: VoiceState,
    onMicTap: () -> Unit,
    liveTranscript: String? = null  // ✅ NEW: shows what user says live
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            // ---------- Dynamic helper text ----------
            val label = when (voiceState) {
                VoiceState.Listening ->
                    if (liveTranscript.isNullOrBlank()) "Listening…" else liveTranscript
                VoiceState.Processing -> "Processing…"
                VoiceState.Idle -> "Describe your symptoms or tap the mic…"
            }

            Text(
                text = label,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // ---------- Mic Button + Pulsing Waves ----------
            Box(contentAlignment = Alignment.Center) {
                if (voiceState == VoiceState.Listening) PulsingWaves()
                FilledIconButton(
                    onClick = onMicTap,
                    modifier = Modifier.size(72.dp)
                ) { Icon(Icons.Filled.Mic, contentDescription = "Mic") }
            }
        }
    }
}
