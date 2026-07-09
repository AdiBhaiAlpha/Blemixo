package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.BlemixoDatabase
import com.example.data.models.ChatEntity
import com.example.data.models.MessageEntity
import com.example.data.models.UserEntity
import com.example.data.repository.BlemixoRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class BlemixoViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: BlemixoRepository

    init {
        val db = BlemixoDatabase.getDatabase(application)
        repository = BlemixoRepository(db.userDao(), db.chatDao(), db.messageDao())

        // Check if database is empty and prepopulate
        viewModelScope.launch {
            val user = repository.getUserSync()
            // If user or chats do not exist, prepopulate default contacts and messages
            repository.prepopulateDefaultData()
        }
    }

    // UI States
    val currentUser: StateFlow<UserEntity?> = repository.user.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    private val _currentScreen = MutableStateFlow("login")
    val currentScreen: StateFlow<String> = _currentScreen.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Chats sorted as specified by Room DAO (Pinned first, then last message timestamp)
    val chats: StateFlow<List<ChatEntity>> = combine(
        repository.allChats,
        _searchQuery
    ) { chatList, query ->
        if (query.isEmpty()) {
            chatList
        } else {
            chatList.filter { it.contactName.contains(query, ignoreCase = true) || it.lastMessage.contains(query, ignoreCase = true) }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _activeChatId = MutableStateFlow<Int?>(null)
    val activeChatId: StateFlow<Int?> = _activeChatId.asStateFlow()

    // Reactive active chat
    val activeChat: StateFlow<ChatEntity?> = _activeChatId.flatMapLatest { id ->
        if (id == null) flowOf(null)
        else chats.map { list -> list.find { it.id == id } }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    // Reactive messages list for active chat
    val activeMessages: StateFlow<List<MessageEntity>> = _activeChatId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList())
        else repository.getMessagesForChat(id)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _activeCallChat = MutableStateFlow<ChatEntity?>(null)
    val activeCallChat: StateFlow<ChatEntity?> = _activeCallChat.asStateFlow()

    private val _activeVideoCallChat = MutableStateFlow<ChatEntity?>(null)
    val activeVideoCallChat: StateFlow<ChatEntity?> = _activeVideoCallChat.asStateFlow()

    // Notification banner state
    private val _notification = MutableStateFlow<String?>(null)
    val notification: StateFlow<String?> = _notification.asStateFlow()

    // Navigation helpers
    fun navigateTo(screen: String) {
        _currentScreen.value = screen
    }

    fun openChat(chatId: Int) {
        _activeChatId.value = chatId
        navigateTo("chat")
    }

    fun closeChat() {
        _activeChatId.value = null
        navigateTo("home")
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Authentication Actions
    fun login(phoneNumber: String, pin: String, name: String = "You") {
        viewModelScope.launch {
            val defaultAvatar = "ic_avatar_me"
            val user = UserEntity(
                id = 1,
                phoneNumber = phoneNumber,
                pin = pin,
                name = name,
                avatarUrl = defaultAvatar,
                isLoggedIn = true,
                isDarkMode = currentUser.value?.isDarkMode ?: false
            )
            repository.saveUser(user)
            navigateTo("home")
            showNotification("Successfully logged into Blemixo!")
        }
    }

    fun logout() {
        viewModelScope.launch {
            val user = currentUser.value
            if (user != null) {
                repository.saveUser(user.copy(isLoggedIn = false))
            }
            navigateTo("login")
        }
    }

    // Profile Actions
    fun updateProfile(name: String, bio: String) {
        viewModelScope.launch {
            val user = currentUser.value
            if (user != null) {
                repository.saveUser(user.copy(name = name, bio = bio))
                showNotification("Profile updated successfully")
            }
        }
    }

    // Chat Actions
    fun pinChat(chatId: Int, isPinned: Boolean) {
        viewModelScope.launch {
            repository.pinChat(chatId, isPinned)
            showNotification(if (isPinned) "Chat Pinned to top" else "Chat Unpinned")
        }
    }

    fun muteChat(chatId: Int, isMuted: Boolean) {
        viewModelScope.launch {
            repository.muteChat(chatId, isMuted)
            showNotification(if (isMuted) "Notifications muted" else "Notifications unmuted")
        }
    }

    fun archiveChat(chatId: Int, isArchived: Boolean) {
        viewModelScope.launch {
            repository.archiveChat(chatId, isArchived)
            showNotification(if (isArchived) "Chat archived" else "Chat unarchived")
        }
    }

    fun deleteChat(chat: ChatEntity) {
        viewModelScope.launch {
            repository.deleteChat(chat)
            repository.clearMessages(chat.id)
            showNotification("Chat with ${chat.contactName} deleted")
        }
    }

    // Message Actions
    fun sendTextMessage(text: String) {
        val chatId = _activeChatId.value ?: return
        viewModelScope.launch {
            val message = MessageEntity(
                chatId = chatId,
                text = text,
                isOutgoing = true,
                timestamp = System.currentTimeMillis()
            )
            repository.insertMessage(message)
            triggerContactReply(chatId, text)
        }
    }

    fun sendVoiceMessage(duration: Int) {
        val chatId = _activeChatId.value ?: return
        viewModelScope.launch {
            val message = MessageEntity(
                chatId = chatId,
                text = "Voice Message (${duration}s)",
                isOutgoing = true,
                timestamp = System.currentTimeMillis(),
                messageType = "VOICE",
                mediaDuration = duration
            )
            repository.insertMessage(message)
            triggerContactReply(chatId, "[Voice message]")
        }
    }

    fun sendImageMessage(uri: String) {
        val chatId = _activeChatId.value ?: return
        viewModelScope.launch {
            val message = MessageEntity(
                chatId = chatId,
                text = "📷 Photo",
                isOutgoing = true,
                timestamp = System.currentTimeMillis(),
                messageType = "IMAGE",
                mediaUri = uri
            )
            repository.insertMessage(message)
            triggerContactReply(chatId, "[Photo]")
        }
    }

    fun sendVideoMessage(uri: String, duration: Int) {
        val chatId = _activeChatId.value ?: return
        viewModelScope.launch {
            val message = MessageEntity(
                chatId = chatId,
                text = "🎥 Video",
                isOutgoing = true,
                timestamp = System.currentTimeMillis(),
                messageType = "VIDEO",
                mediaUri = uri,
                mediaDuration = duration
            )
            repository.insertMessage(message)
            triggerContactReply(chatId, "[Video]")
        }
    }

    fun sendDocumentMessage(fileName: String, size: String) {
        val chatId = _activeChatId.value ?: return
        viewModelScope.launch {
            val message = MessageEntity(
                chatId = chatId,
                text = fileName,
                isOutgoing = true,
                timestamp = System.currentTimeMillis(),
                messageType = "DOCUMENT",
                fileSize = size
            )
            repository.insertMessage(message)
            triggerContactReply(chatId, "[Document]")
        }
    }

    // Mock Contact Replies for high interactivity
    private suspend fun triggerContactReply(chatId: Int, userMsg: String) {
        // Find chat metadata
        val chat = chats.value.find { it.id == chatId } ?: return

        // Set status to typing
        val typingChat = chat.copy(onlineStatus = "Typing...")
        repository.updateChat(typingChat)

        delay(1500) // Simulated typing delay

        val replyText = when {
            userMsg.contains("hello", ignoreCase = true) || userMsg.contains("hi", ignoreCase = true) -> {
                "Hi there! Glad you contacted me. How are you?"
            }
            userMsg.contains("blemixo", ignoreCase = true) -> {
                "Blemixo is amazing! The UI supports smooth transitions, dynamic theme colors, and Glassmorphic cards."
            }
            userMsg.contains("[Voice", ignoreCase = true) || userMsg.contains("voice", ignoreCase = true) -> {
                "🎤 Nice! Let me listen to that voice message real quick."
            }
            userMsg.contains("[Photo", ignoreCase = true) || userMsg.contains("photo", ignoreCase = true) -> {
                "Wow, that photo looks crystal clear! Love the lighting."
            }
            userMsg.contains("[Document", ignoreCase = true) || userMsg.contains("document", ignoreCase = true) -> {
                "Got the document! I will review the Blemixo specification sheets now."
            }
            userMsg.contains("call", ignoreCase = true) -> {
                "Feel free to initiate a mock voice or video call! Just tap the call icons in the top bar."
            }
            else -> {
                listOf(
                    "That sounds perfect! Let's touch base later today.",
                    "Oh wow, I absolutely agree with that.",
                    "Haha that's amazing! Tell me more about it.",
                    "Could you send me the details of that project?",
                    "Understood. I will let you know once I'm done."
                ).random()
            }
        }

        val replyMessage = MessageEntity(
            chatId = chatId,
            text = replyText,
            isOutgoing = false,
            timestamp = System.currentTimeMillis()
        )
        repository.insertMessage(replyMessage)

        // Restore online status
        val onlineChat = chat.copy(onlineStatus = "Online")
        repository.updateChat(onlineChat)
    }

    // Call Actions
    fun startVoiceCall(chat: ChatEntity) {
        _activeCallChat.value = chat
        navigateTo("call")
    }

    fun endVoiceCall() {
        _activeCallChat.value = null
        navigateTo("chat")
    }

    fun startVideoCall(chat: ChatEntity) {
        _activeVideoCallChat.value = chat
        navigateTo("videocall")
    }

    fun endVideoCall() {
        _activeVideoCallChat.value = null
        navigateTo("chat")
    }

    // Theme control
    fun toggleDarkMode(isDarkMode: Boolean) {
        viewModelScope.launch {
            repository.updateDarkMode(isDarkMode)
        }
    }

    // Notification control
    fun showNotification(message: String) {
        viewModelScope.launch {
            _notification.value = message
            delay(3000)
            if (_notification.value == message) {
                _notification.value = null
            }
        }
    }

    fun dismissNotification() {
        _notification.value = null
    }
}
