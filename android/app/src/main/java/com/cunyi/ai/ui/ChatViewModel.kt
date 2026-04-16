package com.cunyi.ai.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cunyi.ai.model.ChatMessage
import com.cunyi.ai.model.ChatSession
import com.cunyi.ai.model.LiteRTEngine
import com.cunyi.ai.model.MessageRole
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isGenerating: Boolean = false,
    val isDownloading: Boolean = false,
    val isLoadingModel: Boolean = false,
    val isModelReady: Boolean = false,
    val downloadProgress: Float = 0f,
    val downloadedMB: Float = 0f,
    val totalMB: Float = 0f,
    val downloadError: String? = null,
    val currentSession: ChatSession? = null
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val liteRTEngine: LiteRTEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val session = ChatSession()

    init {
        checkModelStatus()
    }

    private fun checkModelStatus() {
        viewModelScope.launch {
            val exists = liteRTEngine.modelFile.exists()
            _uiState.update { it.copy(isModelReady = liteRTEngine.isModelReady) }

            if (exists && liteRTEngine.isModelReady) {
                _uiState.update { it.copy(isModelReady = true) }
            } else if (exists) {
                // 模型文件存在但未加载，尝试加载
                loadModel()
            }
        }
    }

    fun startModelDownload() {
        if (_uiState.value.isDownloading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isDownloading = true, downloadError = null, downloadProgress = 0f) }

            val result = liteRTEngine.initialize()

            result.onSuccess {
                _uiState.update {
                    it.copy(
                        isDownloading = false,
                        isLoadingModel = true,
                        downloadProgress = 1f
                    )
                }
                // 模型已自动下载并加载，继续加载到引擎
                _uiState.update { it.copy(isModelReady = true, isLoadingModel = false) }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isDownloading = false,
                        isLoadingModel = false,
                        downloadError = error.message ?: "下载失败"
                    )
                }
            }
        }
    }

    private suspend fun loadModel() {
        _uiState.update { it.copy(isLoadingModel = true) }
        liteRTEngine.initialize()
        _uiState.update { it.copy(isLoadingModel = false, isModelReady = true) }
    }

    fun updateInput(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun sendMessage() {
        val text = _uiState.value.inputText.trim()
        if (text.isBlank() || _uiState.value.isGenerating) return

        // 添加用户消息
        val userMsg = ChatMessage(role = MessageRole.USER, content = text)
        session.messages.add(userMsg)
        _uiState.update {
            it.copy(
                messages = session.messages.toList(),
                inputText = "",
                isGenerating = true
            )
        }

        viewModelScope.launch {
            // 构建 prompt（村医角色提示 + 历史消息）
            val systemPrompt = """你是一位专业、耐心、温暖的乡村医生。请根据你的医学知识，用简洁清晰的语言回答用户的问题。注意：你的回答仅供参考，不能替代专业医疗诊断和治疗建议，如有严重症状请及时就医。"""
            val historyText = session.messages
                .filter { it.role == MessageRole.USER || it.role == MessageRole.ASSISTANT }
                .takeLast(10)
                .joinToString("\n") {
                    if (it.role == MessageRole.USER) "用户：${it.content}"
                    else "医生：${it.content}"
                }
            val fullPrompt = "$systemPrompt\n\n$historyText\n用户：$text\n医生："

            val result = liteRTEngine.generate(
                prompt = fullPrompt,
                maxTokens = 512,
                temperature = 0.7f
            )

            result.onSuccess { response ->
                val assistantMsg = ChatMessage(role = MessageRole.ASSISTANT, content = response.trim())
                session.messages.add(assistantMsg)
                _uiState.update {
                    it.copy(
                        messages = session.messages.toList(),
                        isGenerating = false
                    )
                }
            }.onFailure { error ->
                val errorMsg = ChatMessage(
                    role = MessageRole.ASSISTANT,
                    content = "抱歉，生成失败：${error.message}"
                )
                session.messages.add(errorMsg)
                _uiState.update {
                    it.copy(messages = session.messages.toList(), isGenerating = false)
                }
            }
        }
    }

    fun clearChat() {
        session.messages.clear()
        _uiState.update { it.copy(messages = emptyList()) }
    }

    override fun onCleared() {
        super.onCleared()
        liteRTEngine.release()
    }
}
