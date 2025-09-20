package com.example.gpplus

import android.os.Bundle
import android.speech.SpeechRecognizer
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gpplus.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GPPlusTheme {
                GPPlusApp()
            }
        }
    }
}

enum class VoiceState { Idle, Listening, Processing }
data class ChatMsg(val isUser: Boolean, val text: String)
enum class AppScreen { Welcome, Chat }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GPPlusApp() {
    var currentScreen by remember { mutableStateOf(AppScreen.Welcome) }
    var messages by remember { mutableStateOf(emptyList<ChatMsg>()) }

    // Navigation drawer state
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            NavigationDrawerContent(
                onItemClick = { item ->
                    scope.launch {
                        drawerState.close()
                        // Handle navigation item clicks here
                        when (item) {
                            "New Chat" -> {
                                messages = emptyList()
                                currentScreen = AppScreen.Welcome
                            }
                            "History" -> {
                                // TODO: Implement chat history
                            }
                            "Settings" -> {
                                // TODO: Implement settings
                            }
                        }
                    }
                }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) {
        when (currentScreen) {
            AppScreen.Welcome -> {
                WelcomeScreen(
                    onFirstMessage = { message ->
                        messages = listOf(ChatMsg(true, message))
                        currentScreen = AppScreen.Chat
                    },
                    onMenuClick = {
                        scope.launch {
                            drawerState.open()
                        }
                    }
                )
            }
            AppScreen.Chat -> {
                GpPlusChatScreen(
                    initialMessages = messages,
                    onMessagesUpdate = { newMessages ->
                        messages = newMessages
                    },
                    onMenuClick = {
                        scope.launch {
                            drawerState.open()
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun NavigationDrawerContent(
    onItemClick: (String) -> Unit
) {
    ModalDrawerSheet(
        modifier = Modifier.width(280.dp)
    ) {
        // Drawer header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(Color(0xFF005EB8)),
            contentAlignment = Alignment.CenterStart
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    Icons.Default.LocalHospital,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "GP Plus",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Your AI Health Assistant",
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Navigation items
        NavigationDrawerItem(
            icon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
            label = { Text("Appointment") },
            selected = false,
            onClick = { onItemClick("Appointment") },
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Receipt, contentDescription = null) },
            label = { Text("Prescription") },
            selected = false,
            onClick = { onItemClick("Prescription") },
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Person, contentDescription = null) },
            label = { Text("Profile") },
            selected = false,
            onClick = { onItemClick("Profile") },
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
            label = { Text("Settings") },
            selected = false,
            onClick = { onItemClick("Settings") },
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Spacer(modifier = Modifier.weight(1f))

        // Bottom section
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Info, contentDescription = null) },
            label = { Text("About") },
            selected = false,
            onClick = { onItemClick("About") },
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Help, contentDescription = null) },
            label = { Text("Help & Support") },
            selected = false,
            onClick = { onItemClick("Help") },
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelcomeScreen(
    onFirstMessage: (String) -> Unit,
    onMenuClick: () -> Unit
) {
    val (asr, startAsr, stopAsr) = rememberAsr()
    val micGranted = remember { mutableStateOf(false) }
    var textInput by remember { mutableStateOf("") }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> micGranted.value = granted }
    )

    LaunchedEffect(asr.state, asr.partialText) {
        if (asr.state == VoiceState.Processing && asr.partialText.isNotBlank()) {
            onFirstMessage(asr.partialText)
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "GP Plus",
                        color = Color(0xFF005EB8),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(
                            Icons.Default.Menu,
                            contentDescription = "Menu",
                            tint = Color(0xFF005EB8)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Main content
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "Your online GP",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        color = Color(0xFF212B32),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Get health advice and information\nfrom an AI-powered assistant",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(48.dp))

                // Doctor illustration card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .background(
                                    Color(0xFF005EB8),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "👩‍⚕️",
                                fontSize = 48.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = "Describe your symptoms",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                color = Color(0xFF212B32),
                                fontWeight = FontWeight.Bold
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "For example, \"I have a sore throat.\"",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.Gray
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))

                // Text input field
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    placeholder = { Text("Describe your symptoms...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (textInput.isNotBlank()) {
                                onFirstMessage(textInput.trim())
                            }
                        }
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Voice input section - fixed positioning
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Show live transcript when listening
                AnimatedVisibility(
                    visible = asr.state == VoiceState.Listening && asr.partialText.isNotBlank(),
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically()
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF005EB8))
                    ) {
                        Text(
                            text = asr.partialText,
                            color = Color.White,
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // Voice input button with animation
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (asr.state == VoiceState.Listening) {
                        PulsingWaves()
                    }

                    FloatingActionButton(
                        onClick = {
                            if (!micGranted.value) {
                                permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                                return@FloatingActionButton
                            }

                            when (asr.state) {
                                VoiceState.Idle -> startAsr()
                                VoiceState.Listening -> stopAsr()
                                VoiceState.Processing -> { /* Wait */ }
                            }
                        },
                        containerColor = Color(0xFF005EB8),
                        contentColor = Color.White,
                        modifier = Modifier.size(64.dp)
                    ) {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = "Voice input",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GpPlusChatScreen(
    initialMessages: List<ChatMsg> = emptyList(),
    onMessagesUpdate: (List<ChatMsg>) -> Unit = {},
    onMenuClick: () -> Unit
) {
    var messages by remember { mutableStateOf(initialMessages) }
    val (asr, startAsr, stopAsr) = rememberAsr()
    val micGranted = remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> micGranted.value = granted }
    )

    LaunchedEffect(initialMessages) {
        if (initialMessages.size == 1) {
            delay(800)
            val aiResponse = mockAiReply(initialMessages.first().text)
            messages = messages + ChatMsg(false, aiResponse)
            onMessagesUpdate(messages)
        }
    }

    LaunchedEffect(asr.state, asr.partialText) {
        if (asr.state == VoiceState.Processing && asr.partialText.isNotBlank()) {
            val userMessage = asr.partialText
            messages = messages + ChatMsg(true, userMessage)
            onMessagesUpdate(messages)

            delay(100)
            messages = messages + ChatMsg(false, mockAiReply(userMessage))
            onMessagesUpdate(messages)
            startAsr()
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                scrollBehavior = scrollBehavior,
                title = {
                    Column {
                        Text("GP Plus")
                        Text(
                            "GP Assistant",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(
                            Icons.Default.Menu,
                            contentDescription = "Menu"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp
            ) {
                BottomMicBar(
                    voiceState = asr.state,
                    onMicTap = {
                        if (!micGranted.value) {
                            permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                            return@BottomMicBar
                        }

                        when (asr.state) {
                            VoiceState.Idle -> startAsr()
                            VoiceState.Listening -> stopAsr()
                            VoiceState.Processing -> startAsr()
                        }
                    },
                    liveTranscript = if (asr.state == VoiceState.Listening) asr.partialText else null
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            items(messages) { msg ->
                ChatBubble(msg)
            }
        }
    }
}

@Composable
fun ChatBubble(msg: ChatMsg) {
    val backgroundColor = if (msg.isUser) Color(0xFF005EB8) else Color(0xFFF5F5F5)
    val textColor = if (msg.isUser) Color.White else Color(0xFF212B32)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(backgroundColor)
                .padding(12.dp)
                .widthIn(max = 320.dp)
        ) {
            Text(text = msg.text, color = textColor)
        }
    }
}

@Composable
fun rememberAsr(): Triple<AsrState, () -> Unit, () -> Unit> {
    val context = LocalContext.current
    var uiState by remember { mutableStateOf(AsrState()) }

    val controller = remember {
        SpeechRecognizer.createSpeechRecognizer(context).let { speechRecognizer ->
            AsrController(speechRecognizer).apply {
                attachListener()
            }
        }
    }

    LaunchedEffect(Unit) {
        controller.setCallbacks(
            onPartial = { partialText ->
                uiState = uiState.copy(
                    state = VoiceState.Listening,
                    partialText = partialText,
                    error = null
                )
            },
            onFinal = { finalText ->
                uiState = uiState.copy(
                    state = VoiceState.Processing,
                    partialText = finalText,
                    error = null
                )
            },
            onError = { errorMsg ->
                uiState = uiState.copy(
                    state = VoiceState.Idle,
                    error = errorMsg,
                    partialText = ""
                )
            },
            onRms = { audioLevel ->
                uiState = uiState.copy(audioLevel01 = audioLevel)
            }
        )
    }

    val startListening = {
        uiState = uiState.copy(state = VoiceState.Listening, partialText = "", error = null)
        controller.start()
    }

    val stopListening = {
        controller.stop()
    }

    DisposableEffect(Unit) {
        onDispose {
            controller.destroy()
        }
    }

    return Triple(uiState, startListening, stopListening)
}

private fun mockAiReply(userMessage: String): String {
    val lowercaseMessage = userMessage.lowercase()

    return when {
        "chest" in lowercaseMessage ||
                "breath" in lowercaseMessage ||
                "breathing" in lowercaseMessage ->
            "⚠️ If you have chest pain or shortness of breath, please contact NHS 111 immediately."

        "fever" in lowercaseMessage && "headache" in lowercaseMessage ->
            "Sounds like a short-term illness. Hydrate and rest. If symptoms worsen or persist >3 days, contact your GP or NHS 111."

        "sore throat" in lowercaseMessage ->
            "Sorry you're unwell. Do you also have chest pain or trouble breathing?"

        else ->
            "Thanks for describing your symptoms. Can you tell me more about when they started?"
    }
}