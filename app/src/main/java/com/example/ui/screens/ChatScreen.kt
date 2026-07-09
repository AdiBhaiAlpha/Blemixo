package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.ChatEntity
import com.example.data.models.MessageEntity
import com.example.ui.BlemixoViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(viewModel: BlemixoViewModel) {
    val chat by viewModel.activeChat.collectAsState()
    val messages by viewModel.activeMessages.collectAsState()

    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var inputText by remember { mutableStateOf("") }
    var showAttachments by remember { mutableStateOf(false) }
    var showEmojiPicker by remember { mutableStateOf(false) }

    // Voice Message simulated state
    var isRecordingVoice by remember { mutableStateOf(false) }
    var recordSeconds by remember { mutableStateOf(0) }

    // Media viewer overlays
    var activeViewerMediaUrl by remember { mutableStateOf<String?>(null) }
    var activeViewerMediaType by remember { mutableStateOf<String?>(null) }

    // Auto scroll to end when messages load/change
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Voice recording timer
    LaunchedEffect(isRecordingVoice) {
        if (isRecordingVoice) {
            recordSeconds = 0
            while (isRecordingVoice) {
                delay(1000)
                recordSeconds++
            }
        }
    }

    if (chat == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No active conversation", color = MaterialTheme.colorScheme.onBackground)
        }
        return
    }

    val contactName = chat!!.contactName
    val isOnline = chat!!.onlineStatus == "Online" || chat!!.onlineStatus == "Typing..."
    val isTyping = chat!!.onlineStatus == "Typing..."

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Chat Header
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Avatar Circle
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = contactName.firstOrNull()?.toString()?.uppercase() ?: "B",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Name and Status
                        Column {
                            Text(
                                text = contactName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isOnline) {
                                    Box(
                                        modifier = Modifier
                                            .padding(end = 5.dp)
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(StatusSuccess)
                                    )
                                }
                                Text(
                                    text = chat!!.onlineStatus,
                                    fontSize = 11.sp,
                                    color = if (isTyping) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    fontWeight = if (isTyping) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.closeChat() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.startVoiceCall(chat!!) }) {
                        Icon(Icons.Default.Call, contentDescription = "Voice Call", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { viewModel.startVideoCall(chat!!) }) {
                        Icon(Icons.Default.Videocam, contentDescription = "Video Call", tint = MaterialTheme.colorScheme.primary)
                    }

                    // More dropdown menu
                    var showMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showMenu = !showMenu }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Options")
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text(if (chat!!.isMuted) "Unmute Chat" else "Mute Chat") },
                                onClick = {
                                    viewModel.muteChat(chat!!.id, !chat!!.isMuted)
                                    showMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (chat!!.isPinned) "Unpin Chat" else "Pin Chat") },
                                onClick = {
                                    viewModel.pinChat(chat!!.id, !chat!!.isPinned)
                                    showMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (chat!!.isArchived) "Unarchive" else "Archive") },
                                onClick = {
                                    viewModel.archiveChat(chat!!.id, !chat!!.isArchived)
                                    showMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete Chat") },
                                onClick = {
                                    viewModel.deleteChat(chat!!)
                                    viewModel.closeChat()
                                    showMenu = false
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.border(0.5.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))
            )

            // Conversation Messages Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = if (MaterialTheme.colorScheme.primary == Color(0xFF25D366)) {
                                listOf(ChatBgDark, BgDark)
                            } else {
                                listOf(ChatBgLight, BgLight)
                            }
                        )
                    )
            ) {
                // Background pattern subtle lines
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(messages) { message ->
                        MessageBubbleItem(
                            message = message,
                            onMediaClick = { url, type ->
                                activeViewerMediaType = type
                                activeViewerMediaUrl = url
                            }
                        )
                    }
                }
            }

            // Message Composer / Input Area
            Surface(
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column {
                    // Quick Attachment bar
                    AnimatedVisibility(
                        visible = showAttachments,
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                .padding(vertical = 12.dp)
                        ) {
                            AttachmentItem(
                                icon = Icons.Default.Image,
                                label = "Photo",
                                color = Color(0xFF3B82F6),
                                onClick = {
                                    viewModel.sendImageMessage("img_sample_photo")
                                    showAttachments = false
                                }
                            )
                            AttachmentItem(
                                icon = Icons.Default.Videocam,
                                label = "Video",
                                color = Color(0xFFEF4444),
                                onClick = {
                                    viewModel.sendVideoMessage("vid_sample_video", 14)
                                    showAttachments = false
                                }
                            )
                            AttachmentItem(
                                icon = Icons.Default.Description,
                                label = "Document",
                                color = Color(0xFFA855F7),
                                onClick = {
                                    viewModel.sendDocumentMessage("SpecSheet_Blemixo.pdf", "1.4 MB")
                                    showAttachments = false
                                }
                            )
                        }
                    }

                    // Emoji Selector Layout
                    AnimatedVisibility(visible = showEmojiPicker) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                .padding(8.dp)
                        ) {
                            val emojis = listOf(
                                "😀", "😃", "😄", "😁", "😆", "😅", "😂", "🤣", "😊", "😇",
                                "🙂", "🙃", "😉", "😌", "😍", "🥰", "😘", "😗", "😙", "😚",
                                "😋", "😛", "😝", "😜", "🤪", "🤨", "🧐", "🤓", "😎", "🤩",
                                "🥳", "😏", "😒", "😞", "😔", "😟", "😕", "🙁", "☹️", "😣",
                                "👍", "👎", "👌", "🔥", "💯", "💖", "⚡", "👏", "🎉", "🙌"
                            )
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                item {
                                    FlowRow(
                                        maxItemsInEachRow = 10,
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        emojis.forEach { emoji ->
                                            Text(
                                                text = emoji,
                                                fontSize = 28.sp,
                                                modifier = Modifier
                                                    .clickable {
                                                        inputText += emoji
                                                    }
                                                    .padding(6.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Main Row input
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isRecordingVoice) {
                            // Simulated Recording HUD
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(StatusDanger.copy(alpha = 0.1f))
                                    .border(1.dp, StatusDanger.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(StatusDanger)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Recording Voice Note... ${String.format("%02d", recordSeconds)}s",
                                    color = StatusDanger,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "Cancel",
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.clickable { isRecordingVoice = false }
                                )
                            }
                        } else {
                            // Left Emoji Icon
                            IconButton(onClick = {
                                showEmojiPicker = !showEmojiPicker
                                showAttachments = false
                            }) {
                                Icon(
                                    if (showEmojiPicker) Icons.Default.Keyboard else Icons.Default.SentimentSatisfiedAlt,
                                    contentDescription = "Emoji selector",
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }

                            // Input text bar with 14px rounded corners
                            OutlinedTextField(
                                value = inputText,
                                onValueChange = { inputText = it },
                                placeholder = { Text("Message Blemixo...") },
                                shape = RoundedCornerShape(14.dp),
                                singleLine = false,
                                maxLines = 4,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("chat_input_text_field")
                            )

                            // Attachment Icon
                            IconButton(onClick = {
                                showAttachments = !showAttachments
                                showEmojiPicker = false
                            }) {
                                Icon(
                                    Icons.Default.AttachFile,
                                    contentDescription = "Attachment bar",
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Send or Mic button (Press and release for Voice!)
                        if (inputText.trim().isNotEmpty()) {
                            FloatingActionButton(
                                onClick = {
                                    viewModel.sendTextMessage(inputText)
                                    inputText = ""
                                    showEmojiPicker = false
                                },
                                containerColor = MaterialTheme.colorScheme.primary,
                                shape = CircleShape,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("send_button")
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send text",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        } else {
                            FloatingActionButton(
                                onClick = {
                                    if (isRecordingVoice) {
                                        // Save recording
                                        isRecordingVoice = false
                                        viewModel.sendVoiceMessage(if (recordSeconds == 0) 3 else recordSeconds)
                                    } else {
                                        isRecordingVoice = true
                                    }
                                },
                                containerColor = if (isRecordingVoice) StatusSuccess else MaterialTheme.colorScheme.primary,
                                shape = CircleShape,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("mic_button")
                            ) {
                                Icon(
                                    if (isRecordingVoice) Icons.Default.Check else Icons.Default.Mic,
                                    contentDescription = "Voice recorder",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Expanded full screen image / media viewer
        if (activeViewerMediaUrl != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.92f))
                    .clickable { activeViewerMediaUrl = null },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (activeViewerMediaType == "IMAGE") {
                        // High-fidelity graphic mock layout
                        Box(
                            modifier = Modifier
                                .size(300.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.DarkGray)
                                .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(20.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(80.dp))
                        }
                    } else if (activeViewerMediaType == "VIDEO") {
                        Box(
                            modifier = Modifier
                                .size(width = 320.dp, height = 180.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.DarkGray)
                                .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(20.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(60.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Viewing Sent Media (Tap to Close)",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowRow(
    maxItemsInEachRow: Int,
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = horizontalArrangement
        ) {
            // Emulate flow behavior
            content()
        }
    }
}

@Composable
fun MessageBubbleItem(message: MessageEntity, onMediaClick: (String, String) -> Unit) {
    val formatter = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val timeString = formatter.format(Date(message.timestamp))

    val alignEnd = message.isOutgoing
    val bubbleColor = if (alignEnd) {
        if (MaterialTheme.colorScheme.primary == Color(0xFF25D366)) OutgoingBubbleDark else OutgoingBubbleLight
    } else {
        if (MaterialTheme.colorScheme.primary == Color(0xFF25D366)) IncomingBubbleDark else IncomingBubbleLight
    }

    val bubbleTextColor = if (alignEnd) {
        if (MaterialTheme.colorScheme.primary == Color(0xFF25D366)) Color.White else TextPrimaryLight
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        contentAlignment = if (alignEnd) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Card(
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (alignEnd) 18.dp else 4.dp,
                bottomEnd = if (alignEnd) 4.dp else 18.dp
            ),
            colors = CardDefaults.cardColors(containerColor = bubbleColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier
                .widthIn(max = 280.dp)
                .testTag(if (alignEnd) "outgoing_message_bubble" else "incoming_message_bubble")
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                // Media elements checking
                when (message.messageType) {
                    "IMAGE" -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Gray.copy(alpha = 0.3f))
                                .clickable { onMediaClick(message.mediaUri, "IMAGE") },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Image, contentDescription = "Image preview", tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                    "VIDEO" -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Gray.copy(alpha = 0.3f))
                                .clickable { onMediaClick(message.mediaUri, "VIDEO") },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.PlayCircleFilled, contentDescription = "Video preview", tint = Color.White, modifier = Modifier.size(48.dp))
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                    "DOCUMENT" -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(alpha = 0.05f))
                                .padding(8.dp)
                        ) {
                            Icon(Icons.Default.Description, contentDescription = "PDF File", tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(message.text, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(message.fileSize, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            }
                            Icon(Icons.Default.Download, contentDescription = "Download file", modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                    "VOICE" -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            IconButton(onClick = {}, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Play voice note", tint = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            // Simple mock waveform
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                val bars = listOf(12, 24, 16, 32, 10, 20, 18, 28, 14, 22)
                                bars.forEach { height ->
                                    Box(
                                        modifier = Modifier
                                            .width(2.dp)
                                            .height(height.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${message.mediaDuration}s",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }

                // Bubble message text body
                if (message.messageType == "TEXT") {
                    Text(
                        text = message.text,
                        fontSize = 15.sp,
                        color = bubbleTextColor,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }

                // Time stamp & tick status
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = timeString,
                        fontSize = 10.sp,
                        color = if (alignEnd) {
                            if (MaterialTheme.colorScheme.primary == Color(0xFF25D366)) Color.White.copy(alpha = 0.6f) else TextSecondaryLight
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        }
                    )
                    if (alignEnd) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = "Read status",
                            tint = if (message.isRead) Color(0xFF34D399) else Color.Gray,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AttachmentItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, color: Color, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = Color.White)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}
