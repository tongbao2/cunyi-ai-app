package com.cunyi.ai.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cunyi.ai.model.ChatMessage
import com.cunyi.ai.model.MessageRole
import com.cunyi.ai.ui.theme.CunyiGreen
import com.cunyi.ai.ui.theme.CunyiGreenDark
import com.cunyi.ai.ui.theme.CunyiGreenLight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(viewModel: ChatViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("村医AI", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            when {
                                uiState.isModelReady -> " Gemma 4 · 本地运行"
                                uiState.isDownloading -> "下载中 ${(uiState.downloadProgress * 100).toInt()}%"
                                uiState.isLoadingModel -> "加载模型中..."
                                else -> "初始化中..."
                            },
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CunyiGreen
                ),
                actions = {
                    if (uiState.isDownloading || uiState.isLoadingModel) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(8.dp).size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    }
                    IconButton(onClick = { viewModel.clearChat() }) {
                        Icon(Icons.Default.Delete, contentDescription = "清空对话", tint = Color.White)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // 首次运行引导下载界面
            if (!uiState.isModelReady && !uiState.isDownloading && !uiState.isLoadingModel && uiState.downloadError == null) {
                WelcomeCard(
                    onStartDownload = { viewModel.startModelDownload() },
                    modifier = Modifier.weight(1f)
                )
            } else if (uiState.isDownloading) {
                DownloadProgressCard(
                    progress = uiState.downloadProgress,
                    downloadedMB = uiState.downloadedMB,
                    totalMB = uiState.totalMB,
                    modifier = Modifier.weight(1f)
                )
            } else if (uiState.isLoadingModel) {
                LoadingModelCard(modifier = Modifier.weight(1f))
            } else if (uiState.downloadError != null) {
                ErrorCard(
                    error = uiState.downloadError!!,
                    onRetry = { viewModel.startModelDownload() },
                    modifier = Modifier.weight(1f)
                )
            } else {
                // 聊天消息列表
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (uiState.messages.isEmpty()) {
                        item {
                            EmptyChatHint()
                        }
                    }
                    items(uiState.messages, key = { it.id }) { message ->
                        MessageBubble(message = message)
                    }
                    if (uiState.isGenerating) {
                        item {
                            TypingIndicator()
                        }
                    }
                }
            }

            // 输入区域
            MessageInput(
                inputText = uiState.inputText,
                onInputChange = viewModel::updateInput,
                onSend = {
                    focusManager.clearFocus()
                    viewModel.sendMessage()
                },
                isGenerating = uiState.isGenerating,
                isModelReady = uiState.isModelReady,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun WelcomeCard(onStartDownload: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.LocalHospital,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = CunyiGreen
        )
        Spacer(Modifier.height(24.dp))
        Text("欢迎使用村医AI", fontWeight = FontWeight.Bold, fontSize = 24.sp)
        Spacer(Modifier.height(8.dp))
        Text(
            "基于 Gemma 4 的本地离线 AI 助手\n完全在本地运行，保护隐私",
            textAlign = TextAlign.Center,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = CunyiGreenLight.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CloudDownload, null, tint = CunyiGreen, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("首次运行将自动下载模型（约 2.5 GB）", fontSize = 13.sp)
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.WifiOff, null, tint = CunyiGreen, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("下载后完全离线使用，无需网络", fontSize = 13.sp)
                }
            }
        }
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onStartDownload,
            colors = ButtonDefaults.buttonColors(containerColor = CunyiGreen),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Icon(Icons.Default.Download, null)
            Spacer(Modifier.width(8.dp))
            Text("开始下载模型", fontSize = 16.sp)
        }
    }
}

@Composable
fun DownloadProgressCard(
    progress: Float, downloadedMB: Float, totalMB: Float, modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.size(100.dp),
            strokeWidth = 8.dp,
            color = CunyiGreen
        )
        Spacer(Modifier.height(24.dp))
        Text("${(progress * 100).toInt()}%", fontWeight = FontWeight.Bold, fontSize = 32.sp, color = CunyiGreen)
        Spacer(Modifier.height(8.dp))
        Text("%.1f / %.1f MB".format(downloadedMB, totalMB), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
            color = CunyiGreen
        )
        Spacer(Modifier.height(8.dp))
        Text("正在下载 Gemma 4 模型，请保持网络连接...", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun LoadingModelCard(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(modifier = Modifier.size(80.dp), color = CunyiGreen, strokeWidth = 6.dp)
        Spacer(Modifier.height(24.dp))
        Text("正在加载模型到本地引擎...", fontWeight = FontWeight.Medium, fontSize = 18.sp)
        Spacer(Modifier.height(8.dp))
        Text("首次加载可能需要 1-3 分钟，请耐心等待", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ErrorCard(error: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Error, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(16.dp))
        Text("下载失败", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Spacer(Modifier.height(8.dp))
        Text(error, textAlign = TextAlign.Center, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = CunyiGreen)) {
            Icon(Icons.Default.Refresh, null)
            Spacer(Modifier.width(8.dp))
            Text("重试")
        }
    }
}

@Composable
fun MessageBubble(message: ChatMessage) {
    val isUser = message.role == MessageRole.USER
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Icon(
                Icons.Default.HealthAndSafety, null,
                modifier = Modifier.padding(top = 8.dp, end = 8.dp).size(28.dp),
                tint = CunyiGreen
            )
        }
        Box(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp, topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    )
                )
                .background(if (isUser) CunyiGreen else MaterialTheme.colorScheme.surfaceVariant)
                .padding(12.dp)
        ) {
            Text(
                text = message.content,
                fontSize = 15.sp,
                color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun TypingIndicator() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Icon(Icons.Default.HealthAndSafety, null, modifier = Modifier.padding(top = 8.dp, end = 8.dp).size(28.dp), tint = CunyiGreen)
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text("思考中", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
            var dot by remember { mutableStateOf(".") }
            LaunchedEffect(Unit) {
                while (true) {
                    kotlinx.coroutines.delay(500)
                    dot = if (dot.length >= 3) "." else "$dot."
                }
            }
            Text(dot, color = CunyiGreen, fontSize = 14.sp)
        }
    }
}

@Composable
fun EmptyChatHint() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.HealthAndSafety, null, modifier = Modifier.size(48.dp), tint = CunyiGreen.copy(alpha = 0.5f))
        Spacer(Modifier.height(12.dp))
        Text("村医AI 已就绪", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp)
        Text("输入您的问题，AI 将基于医学知识回答", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), fontSize = 13.sp)
    }
}

@Composable
fun MessageInput(
    inputText: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    isGenerating: Boolean,
    isModelReady: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        tonalElevation = 3.dp,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp).imePadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = onInputChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        if (!isModelReady) "等待模型就绪..."
                        else if (isGenerating) "AI 正在生成..."
                        else "输入您的问题..."
                    )
                },
                enabled = isModelReady && !isGenerating,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { if (inputText.isNotBlank()) onSend() }),
                maxLines = 4,
                shape = RoundedCornerShape(24.dp)
            )
            Spacer(Modifier.width(8.dp))
            FilledIconButton(
                onClick = onSend,
                enabled = inputText.isNotBlank() && isModelReady && !isGenerating,
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = CunyiGreen)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, null, tint = Color.White)
            }
        }
    }
}
