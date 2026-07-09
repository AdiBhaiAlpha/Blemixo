package com.example.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: Int = 1,
    val phoneNumber: String,
    val pin: String,
    val name: String,
    val avatarUrl: String = "",
    val bio: String = "Hey there! I am using Blemixo.",
    val isLoggedIn: Boolean = false,
    val isDarkMode: Boolean = false
) : Serializable

@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val contactName: String,
    val contactPhone: String,
    val avatarResName: String, // e.g. "ic_avatar_1", etc.
    val isPinned: Boolean = false,
    val isMuted: Boolean = false,
    val isArchived: Boolean = false,
    val onlineStatus: String = "Offline", // "Online", "Offline", "Typing", "Last seen 2h ago"
    val lastMessage: String = "",
    val lastMessageTime: Long = System.currentTimeMillis()
) : Serializable

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val chatId: Int,
    val text: String,
    val isOutgoing: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val messageType: String = "TEXT", // "TEXT", "IMAGE", "VIDEO", "DOCUMENT", "VOICE"
    val mediaUri: String = "",
    val mediaDuration: Int = 0, // In seconds (for Voice/Video)
    val fileSize: String = "" // e.g. "2.4 MB" (for Documents)
) : Serializable
