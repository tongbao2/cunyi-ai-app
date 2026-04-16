import SwiftUI
import Combine

// MARK: - ChatViewModel
@MainActor
final class ChatViewModel: ObservableObject {
    @Published var messages: [ChatMessage] = []
    @Published var inputText: String = ""
    @Published var isGenerating: Bool = false
    @Published var isDownloading: Bool = false
    @Published var isLoadingModel: Bool = false
    @Published var isModelReady: Bool = false
    @Published var downloadProgress: Double = 0
    @Published var downloadedMB: Double = 0
    @Published var totalMB: Double = 0
    @Published var downloadError: String? = nil

    private let modelManager = ModelManager.shared
    private var cancellables = Set<AnyCancellable>()

    init() {
        setupBindings()
        modelManager.checkModelStatus()
    }

    private func setupBindings() {
        modelManager.$isModelReady
            .receive(on: DispatchQueue.main)
            .assign(to: &$isModelReady)

        modelManager.$isDownloading
            .receive(on: DispatchQueue.main)
            .assign(to: &$isDownloading)

        modelManager.$isLoadingModel
            .receive(on: DispatchQueue.main)
            .assign(to: &$isLoadingModel)

        modelManager.$downloadProgress
            .receive(on: DispatchQueue.main)
            .assign(to: &$downloadProgress)

        modelManager.$downloadedMB
            .receive(on: DispatchQueue.main)
            .assign(to: &$downloadedMB)

        modelManager.$totalMB
            .receive(on: DispatchQueue.main)
            .assign(to: &$totalMB)

        modelManager.$downloadError
            .receive(on: DispatchQueue.main)
            .assign(to: &$downloadError)
    }

    func startDownload() {
        modelManager.downloadModel()
    }

    func sendMessage() {
        let text = inputText.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !text.isEmpty, !isGenerating else { return }

        // Add user message
        let userMsg = ChatMessage(role: .user, content: text)
        messages.append(userMsg)
        inputText = ""
        isGenerating = true

        // Build prompt with role
        let systemPrompt = "你是一位专业、耐心、温暖的乡村医生。请根据你的医学知识，用简洁清晰的语言回答用户的问题。注意：你的回答仅供参考，不能替代专业医疗诊断和治疗建议，如有严重症状请及时就医。"
        let historyText = messages
            .filter { $0.role == .user || $0.role == .assistant }
            .dropLast() // exclude the one just added
            .map { $0.role == .user ? "用户：\($0.content)" : "医生：\($0.content)" }
            .joined(separator: "\n")
        let fullPrompt = "\(systemPrompt)\n\n\(historyText)\n用户：\(text)\n医生："

        Task {
            do {
                let response = try await modelManager.generate(prompt: fullPrompt, maxTokens: 512, temperature: 0.7)
                let assistantMsg = ChatMessage(role: .assistant, content: response.trimmingCharacters(in: .whitespacesAndNewlines))
                messages.append(assistantMsg)
            } catch {
                let errorMsg = ChatMessage(role: .assistant, content: "抱歉，生成失败：\(error.localizedDescription)")
                messages.append(errorMsg)
            }
            isGenerating = false
        }
    }

    func clearChat() {
        messages.removeAll()
    }
}
