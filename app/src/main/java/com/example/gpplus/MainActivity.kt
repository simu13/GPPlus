package com.example.gpplus

// ---------- Imports ----------
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*        // For animations
import androidx.compose.foundation.Canvas     // For custom concentric rings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*           // Material3 UI components
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.gpplus.ui.theme.GPPlusTheme

// ----------------------------------------------------------
//                APP ENTRY POINT
// ----------------------------------------------------------
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ Wrap entire UI with GPPlusTheme so colors, typography, etc. are applied app-wide
        setContent {
            GPPlusTheme {
                GpPlusChatScreen()   // Load main screen UI
            }
        }
    }
}

// ----------------------------------------------------------
//                STATE & DATA MODELS
// ----------------------------------------------------------

// ✅ Voice states: used to control mic button animation & text
enum class VoiceState { Idle, Listening }

// ✅ Represents one chat message in the conversation list
data class ChatMsg(val isUser: Boolean, val text: String)

// ----------------------------------------------------------
//                MAIN CHAT SCREEN
// ----------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GpPlusChatScreen() {

    // ✅ Holds current mic button state: Idle or Listening
    var voiceState by remember { mutableStateOf(VoiceState.Idle) }

    // ✅ Hardcoded messages to demo chat layout
    val messages = remember {
        listOf(
            ChatMsg(true, "I have a fever and a bad headache."),
            ChatMsg(false, "Sorry you’re unwell. Do you also have chest pain or trouble breathing?")
        )
    }

    // ✅ Scaffold gives us ready-made layout slots: TopBar, Content, BottomBar
    Scaffold(
        // ---------- Top bar ----------
        topBar = {
            TopAppBar(
                title = { Text("GP Plus") },  // App title
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,  // NHS blue
                    titleContentColor = MaterialTheme.colorScheme.onPrimary // White text
                )
            )
        },

        // ---------- Bottom bar ----------
        bottomBar = {
            BottomMicBar(
                voiceState = voiceState,
                onMicTap = {
                    // ✅ Toggle mic state: Idle ↔ Listening when tapped
                    voiceState = if (voiceState == VoiceState.Idle)
                        VoiceState.Listening else VoiceState.Idle
                }
            )
        }
    ) { padding ->

        // ---------- Main chat content ----------
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(padding)     // Avoid overlapping Scaffold bars
                .padding(16.dp),      // Inner padding for content
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { msg -> ChatBubble(msg) }  // Reusable chat bubbles
        }
    }
}

// ----------------------------------------------------------
//                CHAT BUBBLES
// ----------------------------------------------------------
@Composable
fun ChatBubble(msg: ChatMsg) {
    // ✅ NHS blue for user messages, light grey for AI
    val bg = if (msg.isUser) Color(0xFF005EB8) else Color(0xFFF5F5F5)
    // ✅ White text on blue, dark grey text on AI replies
    val fg = if (msg.isUser) Color.White else Color(0xFF212B32)

    // ✅ Align bubbles: user → right, AI → left
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))   // Rounded edges
                .background(bg)
                .padding(12.dp)
                .widthIn(max = 320.dp)             // Max width for readability
        ) {
            Text(msg.text, color = fg)
        }
    }
}

// ----------------------------------------------------------
//                BOTTOM MIC BAR
// ----------------------------------------------------------
@Composable
fun BottomMicBar(
    voiceState: VoiceState,
    onMicTap: () -> Unit
) {
    // ✅ Whole mic area at bottom
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        // ✅ Stack label & mic vertically
        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            // ---------- Dynamic helper text ----------
            Text(
                text = if (voiceState == VoiceState.Listening)
                    "Listening…" else "Describe your symptoms or tap the mic…",
                color = MaterialTheme.colorScheme.outline,  // Muted grey tone
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // ---------- Mic button & waves ----------
            Box(contentAlignment = Alignment.Center) {
                // ✅ Show animated waves when in Listening state
                if (voiceState == VoiceState.Listening) PulsingWaves()

                // ✅ Mic button itself
                FilledIconButton(
                    onClick = onMicTap,
                    modifier = Modifier.size(72.dp)
                ) {
                    Icon(Icons.Filled.Mic, contentDescription = "Mic")
                }
            }
        }
    }
}

// ----------------------------------------------------------
//                PULSING WAVES BEHIND MIC
// ----------------------------------------------------------
@Composable
fun PulsingWaves() {
    // ✅ Infinite transition = loops animation forever
    val infinite = rememberInfiniteTransition(label = "pulse")

    // ---------- First ring: faster, smaller ----------
    val scale1 by infinite.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "s1"
    )

    // ---------- Second ring: slower, larger ----------
    val scale2 by infinite.animateFloat(
        initialValue = 1.15f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "s2"
    )

    // ✅ Container to overlay both rings
    Box(
        modifier = Modifier
            .size(140.dp)
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        CanvasRing(scale = scale2, alpha = 0.25f)   // Outer faint ring
        CanvasRing(scale = scale1, alpha = 0.4f)    // Inner stronger ring
    }
}

// ----------------------------------------------------------
//                SINGLE CANVAS RING
// ----------------------------------------------------------
// ----------------------------------------------------------
//                SINGLE CANVAS RING (FIXED)
// ----------------------------------------------------------
@Composable
fun CanvasRing(scale: Float, alpha: Float) {
    // ✅ Get the color OUTSIDE the Canvas drawing scope
    val ringColor = MaterialTheme.colorScheme.primary.copy(alpha = alpha)

    // ✅ Draws a single hollow circle ring with scaling
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        drawCircle(
            color = ringColor,  // Use the color variable instead of MaterialTheme call
            style = Stroke(width = 8f)  // Hollow circle style
        )
    }
}
