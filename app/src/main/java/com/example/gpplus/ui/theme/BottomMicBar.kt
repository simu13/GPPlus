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
    liveTranscript: String? = null  // ✅ shows what user says live
) {
    Surface( // [SOLUTION] Surface gives proper elevation + bg
        tonalElevation = 3.dp,
        shadowElevation = 4.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(88.dp) // [SOLUTION] fixed height = 72dp mic + 16dp text
                .navigationBarsPadding() // [SOLUTION] avoids overlap with system nav bar
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)
                ),
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
                    modifier = Modifier.padding(bottom = 8.dp) // [SOLUTION] tighter spacing
                )

                // ---------- Mic Button + Pulsing Waves ----------
                Box(contentAlignment = Alignment.Center) {
                    if (voiceState == VoiceState.Listening) PulsingWaves()
                    FilledIconButton(
                        onClick = onMicTap,
                        modifier = Modifier.size(72.dp) // [SOLUTION] consistent mic size
                    ) {
                        Icon(Icons.Filled.Mic, contentDescription = "Mic")
                    }
                }
            }
        }
    }
}
