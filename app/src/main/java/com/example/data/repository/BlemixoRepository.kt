package com.example.data.repository

import com.example.data.local.ChatDao
import com.example.data.local.MessageDao
import com.example.data.local.UserDao
import com.example.data.models.ChatEntity
import com.example.data.models.MessageEntity
import com.example.data.models.UserEntity
import kotlinx.coroutines.flow.Flow

class BlemixoRepository(
    private val userDao: UserDao,
    private val chatDao: ChatDao,
    private val messageDao: MessageDao
) {
    val user: Flow<UserEntity?> = userDao.getUser()
    val allChats: Flow<List<ChatEntity>> = chatDao.getAllChats()

    fun getMessagesForChat(chatId: Int): Flow<List<MessageEntity>> {
        return messageDao.getMessagesForChat(chatId)
    }

    suspend fun getUserSync(): UserEntity? = userDao.getUserSync()

    suspend fun saveUser(user: UserEntity) {
        userDao.insertUser(user)
    }

    suspend fun updateDarkMode(isDarkMode: Boolean) {
        userDao.updateDarkMode(isDarkMode)
    }

    suspend fun insertChat(chat: ChatEntity): Int {
        return chatDao.insertChat(chat).toInt()
    }

    suspend fun updateChat(chat: ChatEntity) {
        chatDao.updateChat(chat)
    }

    suspend fun deleteChat(chat: ChatEntity) {
        chatDao.deleteChat(chat)
    }

    suspend fun pinChat(chatId: Int, isPinned: Boolean) {
        chatDao.pinChat(chatId, isPinned)
    }

    suspend fun muteChat(chatId: Int, isMuted: Boolean) {
        chatDao.muteChat(chatId, isMuted)
    }

    suspend fun archiveChat(chatId: Int, isArchived: Boolean) {
        chatDao.archiveChat(chatId, isArchived)
    }

    suspend fun insertMessage(message: MessageEntity) {
        val msgId = messageDao.insertMessage(message)
        // Also update the last message in the chat
        val chatText = when (message.messageType) {
            "IMAGE" -> "📷 Photo"
            "VIDEO" -> "🎥 Video"
            "VOICE" -> "🎤 Voice message"
            "DOCUMENT" -> "📄 Document"
            else -> message.text
        }
        chatDao.updateLastMessage(message.chatId, chatText, message.timestamp)
    }

    suspend fun clearMessages(chatId: Int) {
        messageDao.clearChatMessages(chatId)
        chatDao.updateLastMessage(chatId, "", System.currentTimeMillis())
    }

    suspend fun prepopulateDefaultData() {
        // Create initial chats if they don't exist
        val existingChats = chatDao.getAllChats()
        // Check if any chat exists
        val defaultChats = listOf(
            ChatEntity(
                id = 1,
                contactName = "Elena Rostova",
                contactPhone = "+1 (555) 234-5678",
                avatarResName = "avatar_1",
                isPinned = true,
                onlineStatus = "Online",
                lastMessage = "Hey! Loved the new Blemixo UI. It's so clean!",
                lastMessageTime = System.currentTimeMillis() - 120000
            ),
            ChatEntity(
                id = 2,
                contactName = "Marcus Thorne",
                contactPhone = "+1 (555) 345-6789",
                avatarResName = "avatar_2",
                isPinned = false,
                onlineStatus = "Typing...",
                lastMessage = "Let's review the mock call designs tonight.",
                lastMessageTime = System.currentTimeMillis() - 600000
            ),
            ChatEntity(
                id = 3,
                contactName = "Blemixo Support",
                contactPhone = "+1 (555) 000-0000",
                avatarResName = "avatar_3",
                isPinned = false,
                onlineStatus = "Online",
                lastMessage = "Welcome to Blemixo! Feel free to test chatting.",
                lastMessageTime = System.currentTimeMillis() - 3600000
            ),
            ChatEntity(
                id = 4,
                contactName = "Sarah Jenkins",
                contactPhone = "+1 (555) 456-7890",
                avatarResName = "avatar_4",
                isPinned = false,
                onlineStatus = "Last seen 10m ago",
                lastMessage = "📄 Design_System_v2.pdf (1.8 MB)",
                lastMessageTime = System.currentTimeMillis() - 7200000
            )
        )

        for (chat in defaultChats) {
            chatDao.insertChat(chat)
        }

        // Add some welcoming messages
        val initialMessages = listOf(
            // Chat 1 - Elena Rostova
            MessageEntity(chatId = 1, text = "Hey there! Are you exploring Blemixo?", isOutgoing = false, timestamp = System.currentTimeMillis() - 300000, isRead = true),
            MessageEntity(chatId = 1, text = "Yes, just checking out the design!", isOutgoing = true, timestamp = System.currentTimeMillis() - 240000, isRead = true),
            MessageEntity(chatId = 1, text = "Hey! Loved the new Blemixo UI. It's so clean!", isOutgoing = false, timestamp = System.currentTimeMillis() - 120000, isRead = false),

            // Chat 2 - Marcus Thorne
            MessageEntity(chatId = 2, text = "Hello Marcus, did you finish the video layouts?", isOutgoing = true, timestamp = System.currentTimeMillis() - 1200000, isRead = true),
            MessageEntity(chatId = 2, text = "Let's review the mock call designs tonight.", isOutgoing = false, timestamp = System.currentTimeMillis() - 600000, isRead = true),

            // Chat 3 - Blemixo Support
            MessageEntity(chatId = 3, text = "Welcome to Blemixo! This is a modern offline-first messaging environment designed with Material 3, glassmorphic accents, and smooth animations.", isOutgoing = false, timestamp = System.currentTimeMillis() - 4000000, isRead = true),
            MessageEntity(chatId = 3, text = "You can send text, voice messages, files, photos, or test pins/mutes/archiving.", isOutgoing = false, timestamp = System.currentTimeMillis() - 3800000, isRead = true),
            MessageEntity(chatId = 3, text = "Welcome to Blemixo! Feel free to test chatting.", isOutgoing = false, timestamp = System.currentTimeMillis() - 3600000, isRead = true),

            // Chat 4 - Sarah Jenkins
            MessageEntity(chatId = 4, text = "Did you check the guidelines?", isOutgoing = false, timestamp = System.currentTimeMillis() - 10000000, isRead = true),
            MessageEntity(chatId = 4, text = "Here is the design documentation we discussed.", isOutgoing = false, timestamp = System.currentTimeMillis() - 7500000, isRead = true),
            MessageEntity(chatId = 4, text = "Design_System_v2.pdf", isOutgoing = false, timestamp = System.currentTimeMillis() - 7200000, isRead = true, messageType = "DOCUMENT", fileSize = "1.8 MB")
        )

        for (msg in initialMessages) {
            messageDao.insertMessage(msg)
        }
    }
}
