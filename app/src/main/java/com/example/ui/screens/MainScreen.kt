package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.ChatEntity
import com.example.ui.BlemixoViewModel
import com.example.ui.theme.StatusSuccess
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: BlemixoViewModel) {
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    var currentTab by remember { mutableStateOf("chats") }
    val chats by viewModel.chats.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    // For selected chat details (adaptive master-detail on tablets)
    val activeChatId by viewModel.activeChatId.collectAsState()

    // Interactive bottom sheet for chat long presses
    var selectedSheetChat by remember { mutableStateOf<ChatEntity?>(null) }
    var showChatOptionsSheet by remember { mutableStateOf(false) }
    var showAddChatDialog by remember { mutableStateOf(false) }

    Row(modifier = Modifier.fillMaxSize()) {
        // Sidebar Navigation Rail (Only for tablets / wide screens)
        if (isTablet) {
            NavigationRail(
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxHeight()
                    .border(0.5.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                // Glassmorphic App Icon
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text("B", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                Spacer(modifier = Modifier.height(36.dp))

                NavigationRailItem(
                    selected = currentTab == "chats",
                    onClick = { currentTab = "chats" },
                    icon = { Icon(Icons.Default.Chat, contentDescription = "Chats") },
                    label = { Text("Chats") },
                    colors = NavigationRailItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary
                    )
                )
                NavigationRailItem(
                    selected = currentTab == "calls",
                    onClick = { currentTab = "calls" },
                    icon = { Icon(Icons.Default.Call, contentDescription = "Calls") },
                    label = { Text("Calls") },
                    colors = NavigationRailItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary
                    )
                )
                NavigationRailItem(
                    selected = currentTab == "profile",
                    onClick = { currentTab = "profile" },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                    label = { Text("Profile") },
                    colors = NavigationRailItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary
                    )
                )
                NavigationRailItem(
                    selected = currentTab == "settings",
                    onClick = { currentTab = "settings" },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") },
                    colors = NavigationRailItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }

        // Main Content Area (Lists & Detail)
        Row(modifier = Modifier.weight(1f)) {
            // Master Column (Lists)
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(if (isTablet && activeChatId != null) 0.45f else 1f)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Toolbar Header
                TopAppBar(
                    title = {
                        Text(
                            text = when (currentTab) {
                                "chats" -> "Blemixo Chats"
                                "calls" -> "Calls History"
                                "profile" -> "Profile"
                                else -> "Settings"
                            },
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.border(0.5.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))
                )

                // Render matching screen component based on currentTab
                Box(modifier = Modifier.weight(1f)) {
                    when (currentTab) {
                        "chats" -> {
                            Box(modifier = Modifier.fillMaxSize()) {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    // Real-time Search input styled with 14px rounded corners
                                    OutlinedTextField(
                                        value = searchQuery,
                                        onValueChange = { viewModel.updateSearchQuery(it) },
                                        placeholder = { Text("Search chats or messages...") },
                                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp)
                                            .testTag("chat_search_input"),
                                        singleLine = true
                                    )

                                    if (chats.isEmpty()) {
                                        Box(
                                            modifier = Modifier.weight(1f).fillMaxWidth(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(
                                                    Icons.Default.ChatBubbleOutline,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                                                    modifier = Modifier.size(56.dp)
                                                )
                                                Spacer(modifier = Modifier.height(12.dp))
                                                Text(
                                                    "No active chats found",
                                                    fontSize = 15.sp,
                                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                                )
                                            }
                                        }
                                    } else {
                                        LazyColumn(
                                            modifier = Modifier.fillMaxSize(),
                                            contentPadding = PaddingValues(bottom = 80.dp) // extra padding to avoid overlapping the FAB
                                        ) {
                                            items(chats) { chat ->
                                                ChatItemRow(
                                                    chat = chat,
                                                    isSelected = chat.id == activeChatId,
                                                    onClick = { viewModel.openChat(chat.id) },
                                                    onLongClick = {
                                                        selectedSheetChat = chat
                                                        showChatOptionsSheet = true
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                // Green WhatsApp-style FAB to start a secure session
                                FloatingActionButton(
                                    onClick = { showAddChatDialog = true },
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(20.dp)
                                        .testTag("add_chat_fab")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AddComment,
                                        contentDescription = "Start Secure Session",
                                        tint = Color.White
                                    )
                                }

                                // Start Secure Chat Alert Dialog
                                if (showAddChatDialog) {
                                    var newContactName by remember { mutableStateOf("") }
                                    var newContactPhone by remember { mutableStateOf("") }
                                    var inputError by remember { mutableStateOf<String?>(null) }

                                    AlertDialog(
                                        onDismissRequest = { showAddChatDialog = false },
                                        title = {
                                            Text(
                                                text = "Start Secure Chat",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 20.sp
                                            )
                                        },
                                        text = {
                                            Column(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Text(
                                                    text = "Enter a contact's name and mobile number to begin a secure encrypted Blemixo session.",
                                                    fontSize = 14.sp,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                                )

                                                OutlinedTextField(
                                                    value = newContactName,
                                                    onValueChange = { newContactName = it },
                                                    label = { Text("Contact Name") },
                                                    leadingIcon = { Icon(Icons.Default.Person, null) },
                                                    shape = RoundedCornerShape(14.dp),
                                                    singleLine = true,
                                                    modifier = Modifier.fillMaxWidth().testTag("new_contact_name")
                                                )

                                                OutlinedTextField(
                                                    value = newContactPhone,
                                                    onValueChange = { newContactPhone = it },
                                                    label = { Text("Phone Number") },
                                                    leadingIcon = { Icon(Icons.Default.Phone, null) },
                                                    shape = RoundedCornerShape(14.dp),
                                                    singleLine = true,
                                                    modifier = Modifier.fillMaxWidth().testTag("new_contact_phone")
                                                )

                                                if (inputError != null) {
                                                    Text(
                                                        text = inputError!!,
                                                        color = MaterialTheme.colorScheme.error,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }
                                            }
                                        },
                                        confirmButton = {
                                            Button(
                                                onClick = {
                                                    if (newContactName.trim().isEmpty() || newContactPhone.trim().isEmpty()) {
                                                        inputError = "Please fill in all fields."
                                                    } else {
                                                        viewModel.createNewChat(newContactName.trim(), newContactPhone.trim())
                                                        showAddChatDialog = false
                                                    }
                                                },
                                                shape = RoundedCornerShape(12.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                            ) {
                                                Text("Start Chat", color = Color.White)
                                            }
                                        },
                                        dismissButton = {
                                            TextButton(onClick = { showAddChatDialog = false }) {
                                                Text("Cancel")
                                            }
                                        }
                                    )
                                }
                            }
                        }
                        "calls" -> {
                            CallsHistoryTab(viewModel)
                        }
                        "profile" -> {
                            ProfileScreen(viewModel)
                        }
                        "settings" -> {
                            SettingsScreen(viewModel)
                        }
                    }
                }
            }

            // Divider for Master-Detail (Only Tablet)
            if (isTablet && activeChatId != null) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f))
                )

                // Detail Column (Active Chat conversation)
                Box(modifier = Modifier.weight(0.55f)) {
                    ChatScreen(viewModel)
                }
            }
        }
    }

    // Interactive Bottom sheet options for chat threads (Mute, Pin, Delete, Archive)
    if (showChatOptionsSheet && selectedSheetChat != null) {
        val sheetChat = selectedSheetChat!!
        AlertDialog(
            onDismissRequest = { showChatOptionsSheet = false },
            title = { Text(text = sheetChat.contactName) },
            text = { Text(text = "Choose action for this conversation thread:") },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Mute / Unmute
                    Button(
                        onClick = {
                            viewModel.muteChat(sheetChat.id, !sheetChat.isMuted)
                            showChatOptionsSheet = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(if (sheetChat.isMuted) Icons.Default.VolumeUp else Icons.Default.VolumeMute, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (sheetChat.isMuted) "Unmute notifications" else "Mute notifications")
                    }

                    // Pin / Unpin
                    Button(
                        onClick = {
                            viewModel.pinChat(sheetChat.id, !sheetChat.isPinned)
                            showChatOptionsSheet = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PushPin, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (sheetChat.isPinned) "Unpin chat" else "Pin chat to top")
                    }

                    // Archive
                    Button(
                        onClick = {
                            viewModel.archiveChat(sheetChat.id, !sheetChat.isArchived)
                            showChatOptionsSheet = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Archive, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (sheetChat.isArchived) "Unarchive chat" else "Archive chat")
                    }

                    // Delete Row
                    Button(
                        onClick = {
                            viewModel.deleteChat(sheetChat)
                            showChatOptionsSheet = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete Conversation")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showChatOptionsSheet = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Bottom Navigation Bar (Only for compact/mobile viewports)
    if (!isTablet) {
        Column {
            Divider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                NavigationBarItem(
                    selected = currentTab == "chats",
                    onClick = { currentTab = "chats" },
                    icon = {
                        BadgedBox(badge = {
                            if (chats.any { !it.isMuted }) {
                                Badge(containerColor = MaterialTheme.colorScheme.primary)
                            }
                        }) {
                            Icon(Icons.Default.Chat, contentDescription = "Chats")
                        }
                    },
                    label = { Text("Chats") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        indicatorColor = MaterialTheme.colorScheme.primary
                    )
                )
                NavigationBarItem(
                    selected = currentTab == "calls",
                    onClick = { currentTab = "calls" },
                    icon = { Icon(Icons.Default.Call, contentDescription = "Calls") },
                    label = { Text("Calls") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        indicatorColor = MaterialTheme.colorScheme.primary
                    )
                )
                NavigationBarItem(
                    selected = currentTab == "profile",
                    onClick = { currentTab = "profile" },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                    label = { Text("Profile") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        indicatorColor = MaterialTheme.colorScheme.primary
                    )
                )
                NavigationBarItem(
                    selected = currentTab == "settings",
                    onClick = { currentTab = "settings" },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        indicatorColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ChatItemRow(
    chat: ChatEntity,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val formatter = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val timeString = formatter.format(Date(chat.lastMessageTime))

    val isOnline = chat.onlineStatus == "Online" || chat.onlineStatus == "Typing..."
    val isTyping = chat.onlineStatus == "Typing..."

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                else Color.Transparent
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("chat_item_row_${chat.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar circle with online indicator ring
        Box(contentAlignment = Alignment.BottomEnd) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                    .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = chat.contactName.firstOrNull()?.toString()?.uppercase() ?: "B",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Online green pulsing indicator dot
            if (isOnline) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.background)
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(StatusSuccess)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Name and Message block
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = chat.contactName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = timeString,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isTyping) "typing..." else chat.lastMessage,
                    fontSize = 13.sp,
                    color = if (isTyping) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    fontWeight = if (isTyping) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                // Icons overlay (Pin, Muted, Archive)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (chat.isMuted) {
                        Icon(
                            Icons.Default.VolumeOff,
                            contentDescription = "Muted",
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    if (chat.isPinned) {
                        Icon(
                            Icons.Default.PushPin,
                            contentDescription = "Pinned",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    if (chat.isArchived) {
                        Icon(
                            Icons.Default.Archive,
                            contentDescription = "Archived",
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CallsHistoryTab(viewModel: BlemixoViewModel) {
    val callLogs by viewModel.callLogs.collectAsState()
    val chats by viewModel.chats.collectAsState()

    if (callLogs.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.PhoneCallback,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text("No call records found", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
            }
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick = { viewModel.clearCallLogs() },
                modifier = Modifier.testTag("clear_calls_button")
            ) {
                Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Clear History", fontSize = 12.sp)
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(callLogs) { log ->
                val formatter = remember { SimpleDateFormat("MMM dd, h:mm a", Locale.getDefault()) }
                val timeString = formatter.format(Date(log.timestamp))

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(log.contactName.firstOrNull()?.toString() ?: "U", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(log.contactName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (log.isMissed) Icons.Default.CallMissed 
                                                      else if (log.isOutgoing) Icons.Default.CallMade 
                                                      else Icons.Default.CallReceived,
                                        contentDescription = null,
                                        tint = if (log.isMissed) MaterialTheme.colorScheme.error 
                                               else StatusSuccess,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (log.durationSeconds > 0) "$timeString (${log.durationSeconds}s)" else "$timeString (Missed)",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }

                        val correspondingChat = chats.find { it.contactName == log.contactName }
                        if (correspondingChat != null) {
                            Row {
                                IconButton(onClick = { viewModel.startVoiceCall(correspondingChat) }) {
                                    Icon(Icons.Default.Call, contentDescription = "Voice Call", tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { viewModel.startVideoCall(correspondingChat) }) {
                                    Icon(Icons.Default.Videocam, contentDescription = "Video Call", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
