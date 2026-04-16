package com.cunyi.ai.model

import kotlinx.serialization.Serializable

@Serializable
data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val role: MessageRole,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
enum class MessageRole {
    USER, ASSISTANT, SYSTEM
}

@Serializable
data class ChatSession(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String = "新对话",
    val messages: MutableList<ChatMessage> = mutableListOf(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class DownloadState(
    val progress: Float = 0f,
    val status: DownloadStatus = DownloadStatus.IDLE,
    val errorMessage: String? = null
)

enum class DownloadStatus {
    IDLE, CHECKING, DOWNLOADING, LOADING_MODEL, READY, ERROR
}
