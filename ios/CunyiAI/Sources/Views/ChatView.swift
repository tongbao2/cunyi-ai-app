import SwiftUI

struct ChatView: View {
    @StateObject private var vm = ChatViewModel()

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                // Main content area
                Group {
                    if !vm.isModelReady && !vm.isDownloading && !vm.isLoadingModel && vm.downloadError == nil {
                        WelcomeCard(onStartDownload: vm.startDownload)
                    } else if vm.isDownloading {
                        DownloadProgressCard(
                            progress: vm.downloadProgress,
                            downloadedMB: vm.downloadedMB,
                            totalMB: vm.totalMB
                        )
                    } else if vm.isLoadingModel {
                        LoadingModelCard()
                    } else if let error = vm.downloadError {
                        ErrorCard(error: error, onRetry: vm.startDownload)
                    } else {
                        MessageListView(messages: vm.messages, isGenerating: vm.isGenerating)
                    }
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)

                // Input bar
                MessageInputBar(
                    inputText: $vm.inputText,
                    isGenerating: vm.isGenerating,
                    isModelReady: vm.isModelReady,
                    onSend: vm.sendMessage
                )
            }
            .navigationTitle("村医AI")
            .navigationBarTitleDisplayMode(.inline)
            .toolbarBackground(Color("CunyiGreen"), for: .navigationBar)
            .toolbarBackground(.visible, for: .navigationBar)
            .toolbarColorScheme(.dark, for: .navigationBar)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    if !vm.messages.isEmpty {
                        Button {
                            vm.clearChat()
                        } label: {
                            Image(systemName: "trash").foregroundColor(.white)
                        }
                    }
                }
                ToolbarItem(placement: .principal) {
                    VStack(spacing: 2) {
                        Text("村医AI").font(.headline).foregroundColor(.white)
                        Text(statusSubtitle)
                            .font(.caption2)
                            .foregroundColor(.white.opacity(0.8))
                    }
                }
            }
        }
        .preferredColorScheme(.light)
    }

    private var statusSubtitle: String {
        if vm.isModelReady { return "Gemma 4 · 本地运行" }
        if vm.isDownloading { return "下载中 \(Int(vm.downloadProgress * 100))%" }
        if vm.isLoadingModel { return "加载模型中..." }
        return "初始化中..."
    }
}

// MARK: - Welcome Card
struct WelcomeCard: View {
    let onStartDownload: () -> Void

    var body: some View {
        VStack(spacing: 24) {
            Spacer()
            Image(systemName: "cross.case.fill")
                .font(.system(size: 72))
                .foregroundColor(Color("CunyiGreen"))
            Text("欢迎使用村医AI")
                .font(.title).fontWeight(.bold)
            Text("基于 Gemma 4 的本地离线 AI 助手\n完全在本地运行，保护隐私")
                .font(.subheadline)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
            VStack(alignment: .leading, spacing: 12) {
                HStack {
                    Image(systemName: "icloud.and.arrow.down").foregroundColor(Color("CunyiGreen"))
                    Text("首次运行将自动下载模型（约 2.5 GB）")
                        .font(.subheadline)
                }
                HStack {
                    Image(systemName: "wifi.slash").foregroundColor(Color("CunyiGreen"))
                    Text("下载后完全离线使用，无需网络")
                        .font(.subheadline)
                }
            }
            .padding()
            .background(Color("CunyiGreen").opacity(0.08))
            .cornerRadius(12)
            .frame(maxWidth: .infinity)
            .padding(.horizontal, 24)

            Button(action: onStartDownload) {
                HStack {
                    Image(systemName: "arrow.down.circle.fill")
                    Text("开始下载模型")
                }
                .font(.headline)
                .foregroundColor(.white)
                .frame(maxWidth: .infinity)
                .frame(height: 52)
                .background(Color("CunyiGreen"))
                .cornerRadius(12)
            }
            .padding(.horizontal, 24)
            Spacer()
        }
    }
}

// MARK: - Download Progress Card
struct DownloadProgressCard: View {
    let progress: Double
    let downloadedMB: Double
    let totalMB: Double

    var body: some View {
        VStack(spacing: 20) {
            Spacer()
            ZStack {
                Circle()
                    .stroke(Color("CunyiGreen").opacity(0.2), lineWidth: 8)
                Circle()
                    .trim(from: 0, to: progress)
                    .stroke(Color("CunyiGreen"), style: StrokeStyle(lineWidth: 8, lineCap: .round))
                    .rotationEffect(.degrees(-90))
                    .animation(.linear, value: progress)
                Text("\(Int(progress * 100))%")
                    .font(.title).fontWeight(.bold)
                    .foregroundColor(Color("CunyiGreen"))
            }
            .frame(width: 120, height: 120)

            Text(String(format: "%.1f / %.1f MB", downloadedMB, totalMB))
                .font(.subheadline)
                .foregroundColor(.secondary)

            ProgressView(value: progress)
                .tint(Color("CunyiGreen"))
                .padding(.horizontal, 48)

            Text("正在下载 Gemma 4 模型，请保持网络连接...")
                .font(.caption)
                .foregroundColor(.secondary)
            Spacer()
        }
    }
}

// MARK: - Loading Model Card
struct LoadingModelCard: View {
    var body: some View {
        VStack(spacing: 20) {
            Spacer()
            ProgressView()
                .scaleEffect(2)
                .tint(Color("CunyiGreen"))
            Text("正在加载模型到本地引擎...")
                .font(.headline)
            Text("首次加载可能需要 1-3 分钟，请耐心等待")
                .font(.caption)
                .foregroundColor(.secondary)
            Spacer()
        }
    }
}

// MARK: - Error Card
struct ErrorCard: View {
    let error: String
    let onRetry: () -> Void

    var body: some View {
        VStack(spacing: 20) {
            Spacer()
            Image(systemName: "exclamationmark.triangle.fill")
                .font(.system(size: 56))
                .foregroundColor(.red)
            Text("下载失败")
                .font(.headline)
            Text(error)
                .font(.subheadline)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 32)
            Button(action: onRetry) {
                HStack {
                    Image(systemName: "arrow.clockwise")
                    Text("重试")
                }
                .foregroundColor(.white)
                .padding(.horizontal, 24)
                .padding(.vertical, 12)
                .background(Color("CunyiGreen"))
                .cornerRadius(8)
            }
            Spacer()
        }
    }
}

// MARK: - Message List
struct MessageListView: View {
    let messages: [ChatMessage]
    let isGenerating: Bool

    var body: some View {
        ScrollViewReader { proxy in
            ScrollView {
                LazyVStack(spacing: 12) {
                    if messages.isEmpty {
                        EmptyChatHint()
                            .padding(.top, 48)
                    }
                    ForEach(messages) { msg in
                        MessageBubbleView(message: msg)
                            .id(msg.id)
                    }
                    if isGenerating {
                        TypingIndicatorView()
                            .id("typing")
                    }
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 12)
            }
            .onChange(of: messages.count) { _, _ in
                withAnimation {
                    proxy.scrollTo(messages.last?.id ?? "typing", anchor: .bottom)
                }
            }
        }
    }
}

// MARK: - Message Bubble
struct MessageBubbleView: View {
    let message: ChatMessage

    var body: some View {
        HStack {
            if message.role != .user {
                Image(systemName: "cross.case.fill")
                    .foregroundColor(Color("CunyiGreen"))
                    .padding(.top, 8)
            }
            if message.role == .user { Spacer() }
            Text(message.content)
                .font(.body)
                .padding(.horizontal, 14)
                .padding(.vertical, 10)
                .background(
                    message.role == .user
                        ? Color("CunyiGreen")
                        : Color(.systemGray5)
                )
                .foregroundColor(
                    message.role == .user ? .white : .primary
                )
                .clipShape(ChatBubbleShape(isUser: message.role == .user))
                .frame(maxWidth: 280, alignment: message.role == .user ? .trailing : .leading)
            if message.role != .user { Spacer() }
        }
    }
}

// MARK: - Typing Indicator
struct TypingIndicatorView: View {
    @State private var dotCount = 1

    var body: some View {
        HStack {
            Image(systemName: "cross.case.fill")
                .foregroundColor(Color("CunyiGreen"))
                .padding(.top, 8)
            HStack(spacing: 4) {
                Text("思考中")
                Text(String(repeating: ".", count: dotCount))
                    .foregroundColor(Color("CunyiGreen"))
            }
            .font(.subheadline)
            .padding(.horizontal, 16)
            .padding(.vertical, 10)
            .background(Color(.systemGray5))
            .clipShape(RoundedRectangle(cornerRadius: 16))
            Spacer()
        }
        .onAppear {
            Timer.scheduledTimer(withTimeInterval: 0.5, repeats: true) { _ in
                dotCount = (dotCount % 3) + 1
            }
        }
    }
}

// MARK: - Empty Hint
struct EmptyChatHint: View {
    var body: some View {
        VStack(spacing: 8) {
            Image(systemName: "cross.case.fill")
                .font(.system(size: 40))
                .foregroundColor(Color("CunyiGreen").opacity(0.5))
            Text("村医AI 已就绪")
                .font(.subheadline)
                .foregroundColor(.secondary)
            Text("输入您的问题，AI 将基于医学知识回答")
                .font(.caption)
                .foregroundColor(.secondary.opacity(0.7))
        }
        .frame(maxWidth: .infinity)
    }
}

// MARK: - Input Bar
struct MessageInputBar: View {
    @Binding var inputText: String
    let isGenerating: Bool
    let isModelReady: Bool
    let onSend: () -> Void

    var body: some View {
        HStack(spacing: 8) {
            TextField("输入您的问题...", text: $inputText, axis: .vertical)
                .textFieldStyle(.plain)
                .padding(.horizontal, 16)
                .padding(.vertical, 10)
                .background(Color(.systemGray6))
                .clipShape(RoundedRectangle(cornerRadius: 20))
                .lineLimit(1...4)
                .disabled(!isModelReady || isGenerating)
                .onSubmit { if !inputText.isEmpty { onSend() } }

            Button(action: onSend) {
                Image(systemName: "arrow.up.circle.fill")
                    .font(.system(size: 36))
                    .foregroundColor(
                        !inputText.isEmpty && isModelReady && !isGenerating
                            ? Color("CunyiGreen")
                            : .gray
                    )
            }
            .disabled(inputText.isEmpty || !isModelReady || isGenerating)
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 8)
        .background(Color(.systemBackground))
        .overlay(alignment: .top) {
            Divider()
        }
    }
}

// MARK: - Chat Bubble Shape
struct ChatBubbleShape: Shape {
    let isUser: Bool

    func path(in rect: CGRect) -> Path {
        let r = min(rect.height, 20.0)
        var path = Path()
        if isUser {
            path.addRoundedRect(in: rect, cornerSize: CGSize(width: r, height: r))
        } else {
            path.addRoundedRect(in: rect, cornerSize: CGSize(width: r, height: r))
        }
        return path
    }
}
