package com.example.gpplus

// =============================================================================
//                               IMPORTS
// =============================================================================
import android.os.Bundle
import android.speech.SpeechRecognizer
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*        // Animation utilities for mic pulsing
import androidx.compose.foundation.Canvas     // For custom drawing (concentric rings)
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.gpplus.ui.theme.AsrController
import com.example.gpplus.ui.theme.AsrState
import com.example.gpplus.ui.theme.BottomMicBar
import com.example.gpplus.ui.theme.GPPlusTheme
import kotlinx.coroutines.delay

// =============================================================================
//                           APP ENTRY POINT
// =============================================================================
/**
 * Main Activity - Entry point for the GP Plus medical chatbot application
 *
 * This activity sets up the entire UI within the custom GPPlusTheme,
 * which provides NHS-branded colors and typography throughout the app.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Apply custom NHS-blue theme to entire app
        setContent {
            GPPlusTheme {
                GpPlusChatScreen()   // Launch main chat interface
            }
        }
    }
}

// =============================================================================
//                        STATE MODELS & ENUMS
// =============================================================================

/**
 * Voice State Enum - Controls the microphone button behavior and UI feedback
 *
 * - Idle: Ready to start listening (blue mic button)
 * - Listening: Currently recording audio (animated pulsing rings)
 * - Processing: Converting speech to text and generating AI response
 */
enum class VoiceState { Idle, Listening, Processing }

/**
 * Chat Message Data Class - Represents one message bubble in the conversation
 *
 * @param isUser: true = user message (blue, right-aligned), false = AI reply (grey, left-aligned)
 * @param text: The actual message content to display
 */
data class ChatMsg(val isUser: Boolean, val text: String)

// =============================================================================
//                           MAIN CHAT SCREEN
// =============================================================================
/**
 * GpPlusChatScreen - The main UI composable that orchestrates the entire chat interface
 *
 * Features:
 * - NHS-branded top bar with "GP Plus" title
 * - Scrollable chat message list in the center
 * - Voice input bar at bottom with mic button and live transcription
 * - Automatic speech-to-text with real-time feedback
 * - Mock AI responses based on keyword detection
 * - Microphone permission handling
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GpPlusChatScreen() {

    // =========================================================================
    //                        STATE MANAGEMENT
    // =========================================================================

    // Current conversation messages (starts with demo content)
    var messages by remember {
        mutableStateOf(listOf(
            ChatMsg(true, "I have a fever and a bad headache."),
            ChatMsg(false, "Sorry you're unwell. Do you also have chest pain or trouble breathing?")
        ))
    }

    // Speech recognition controller and state management
    val (asr, startAsr, stopAsr) = rememberAsr()

    // Microphone permission state tracking
    val micGranted = remember { mutableStateOf(false) }

    // Permission request launcher for microphone access
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> micGranted.value = granted }
    )

    // =========================================================================
    //                        SIDE EFFECTS
    // =========================================================================

    // Request microphone permission when app first loads
    LaunchedEffect(Unit) {
        permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
    }

    // Handle speech recognition results and trigger AI responses
    LaunchedEffect(asr.state, asr.partialText) {
        // When speech processing completes and we have transcribed text
        if (asr.state == VoiceState.Processing && asr.partialText.isNotBlank()) {
            val userMessage = asr.partialText

            // Add user's message to conversation
            messages = messages + ChatMsg(true, userMessage)

            // Brief delay to simulate AI "thinking" time
            delay(100)

            // Generate and add AI response based on user's words
            messages = messages + ChatMsg(false, mockAiReply(userMessage))
        }
    }

    // =========================================================================
    //                           UI LAYOUT
    // =========================================================================

    // Scaffold provides the overall app structure with top bar, content, and bottom bar
    Scaffold(
        // NHS-branded header bar
        topBar = {
            TopAppBar(
                title = { Text("GP Plus") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,    // NHS blue
                    titleContentColor = MaterialTheme.colorScheme.onPrimary // White text
                )
            )
        },

        // Voice input controls at bottom
        bottomBar = {
            BottomMicBar(
                voiceState = asr.state,
                onMicTap = {
                    // Check microphone permission before starting
                    if (!micGranted.value) {
                        permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                        return@BottomMicBar
                    }

                    // Handle mic button taps based on current state
                    when (asr.state) {
                        VoiceState.Idle -> startAsr()           // Start listening
                        VoiceState.Listening -> stopAsr()       // Stop listening
                        VoiceState.Processing -> Unit            // Wait for completion
                    }
                },
                // Show live transcription while listening
                liveTranscript = if (asr.state == VoiceState.Listening) asr.partialText else null
            )
        }
    ) { padding ->

        // Main chat conversation area
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(padding)         // Respect scaffold bars
                .padding(16.dp),          // Inner content padding
            verticalArrangement = Arrangement.spacedBy(8.dp)  // Space between messages
        ) {
            // Render each message as a chat bubble
            items(messages) { msg ->
                ChatBubble(msg)
            }
        }
    }
}

// =============================================================================
//                          CHAT BUBBLE COMPONENT
// =============================================================================
/**
 * ChatBubble - Renders individual chat messages with proper styling and alignment
 *
 * User messages: Blue background, white text, right-aligned
 * AI messages: Light grey background, dark text, left-aligned
 * Both have rounded corners and max width for readability
 */
@Composable
fun ChatBubble(msg: ChatMsg) {
    // Color scheme based on message sender
    val backgroundColor = if (msg.isUser) Color(0xFF005EB8) else Color(0xFFF5F5F5)  // NHS blue vs light grey
    val textColor = if (msg.isUser) Color.White else Color(0xFF212B32)              // White vs dark grey

    // Message alignment: user messages right, AI messages left
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))       // Rounded bubble appearance
                .background(backgroundColor)
                .padding(12.dp)                        // Inner text padding
                .widthIn(max = 320.dp)                // Prevent overly wide bubbles
        ) {
            Text(text = msg.text, color = textColor)
        }
    }
}

// =============================================================================
//                    SPEECH RECOGNITION INTEGRATION
// =============================================================================
/**
 * rememberAsr - Custom composable hook for speech-to-text functionality
 *
 * Returns a triple containing:
 * - AsrState: Current state and transcription results
 * - startAsr: Function to begin speech recognition
 * - stopAsr: Function to end speech recognition
 *
 * Handles the complete lifecycle of Android's SpeechRecognizer API
 */
@Composable
fun rememberAsr(): Triple<AsrState, () -> Unit, () -> Unit> {
    val context = LocalContext.current
    var uiState by remember { mutableStateOf(AsrState()) }

    // Create speech recognizer controller (persisted across recompositions)
    val controller = remember {
        SpeechRecognizer.createSpeechRecognizer(context).let { speechRecognizer ->
            AsrController(speechRecognizer).apply {
                attachListener()  // Connect Android speech callbacks
            }
        }
    }

    // Set up callbacks when speech events occur
    LaunchedEffect(Unit) {
        controller.setCallbacks(
            // Partial results while user is still speaking
            onPartial = { partialText ->
                uiState = uiState.copy(
                    state = VoiceState.Listening,
                    partialText = partialText,
                    error = null
                )
            },
            // Final result when user stops speaking
            onFinal = { finalText ->
                uiState = uiState.copy(
                    state = VoiceState.Processing,
                    partialText = finalText,
                    error = null
                )
            },
            // Handle speech recognition errors
            onError = { errorMsg ->
                uiState = uiState.copy(
                    state = VoiceState.Idle,
                    error = errorMsg,
                    partialText = ""
                )
            },
            // Audio level for potential future visualizations
            onRms = { audioLevel ->
                uiState = uiState.copy(audioLevel01 = audioLevel)
            }
        )
    }

    // Control functions
    val startListening = {
        uiState = uiState.copy(state = VoiceState.Listening, partialText = "", error = null)
        controller.start()
    }

    val stopListening = {
        controller.stop()
    }

    // Clean up speech recognizer when composable is removed
    DisposableEffect(Unit) {
        onDispose {
            controller.destroy()
        }
    }

    return Triple(uiState, startListening, stopListening)
}

// =============================================================================
//                           AI RESPONSE LOGIC
// =============================================================================

private fun mockAiReply(userMessage: String): String {
    val lowercaseMessage = userMessage.lowercase()

    return when {
        // Emergency symptoms - immediate action required
        "chest" in lowercaseMessage ||
                "breath" in lowercaseMessage ||
                "breathing" in lowercaseMessage ->
            "⚠️ If you have chest pain or shortness of breath, please contact NHS 111 immediately."

        // Common illness symptoms - standard advice
        "fever" in lowercaseMessage && "headache" in lowercaseMessage ->
            "Sounds like a short-term illness. Hydrate and rest. If symptoms worsen or persist >3 days, contact your GP or NHS 111."

        // Default response for other symptoms
        else ->
            "Thanks. Any rash, neck stiffness, or confusion?"
    }
}