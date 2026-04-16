import Foundation
import Combine
import llmfarm_core

/// 村医AI - 模型管理器
/// 首次启动自动下载 GGUF 模型，通过 llmfarm_core（llama.cpp Swift 绑定）运行
final class ModelManager: ObservableObject {
    static let shared = ModelManager()

    @Published var isModelReady: Bool = false
    @Published var isDownloading: Bool = false
    @Published var isLoadingModel: Bool = false
    @Published var downloadProgress: Double = 0
    @Published var downloadedMB: Double = 0
    @Published var totalMB: Double = 0
    @Published var downloadError: String? = nil

    // 模型信息
    let modelURL = "https://hf-mirror.com/unsloth/gemma-4-E2B-it-GGUF/resolve/main/gemma-4-E2B-it-Q4_K_M.gguf"
    let modelFileName = "gemma-4-E2B-it-Q4_K_M.gguf"

    private var model: LLModel? = nil
    private let queue = DispatchQueue(label: "com.cunyi.ai.model", qos: .userInitiated)

    private init() {}

    var modelFile: URL {
        let dir = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
            .appendingPathComponent("models")
        try? FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        return dir.appendingPathComponent(modelFileName)
    }

    /// 检查本地模型状态
    func checkModelStatus() {
        if modelFile.exists() {
            loadModel()
        }
    }

    /// 开始下载模型
    func downloadModel() {
        guard !isDownloading else { return }

        isDownloading = true
        downloadProgress = 0
        downloadedMB = 0
        totalMB = 0
        downloadError = nil

        Task { @MainActor in
            do {
                try await performDownload()
                isDownloading = false
                loadModel()
            } catch {
                isDownloading = false
                downloadError = error.localizedDescription
            }
        }
    }

    private func performDownload() async throws {
        // HEAD 请求获取文件大小
        guard let url = URL(string: modelURL) else { throw URLError(.badURL) }

        var request = URLRequest(url: url)
        request.httpMethod = "HEAD"
        request.timeoutInterval = 30

        let headResponse = try await URLSession.shared.data(for: request)
        guard let httpResponse = headResponse.1 as? HTTPURLResponse,
              httpResponse.statusCode == 200 else {
            throw URLError(.badServerResponse)
        }

        let contentLength = httpResponse.value(forHTTPHeaderField: "Content-Length")
        let total = (contentLength.flatMap { Double($0) } ?? 0) / (1024 * 1024)
        await MainActor.run { self.totalMB = total }

        // 下载文件
        let (tempURL, response) = try await URLSession.shared.download(from: url)

        guard let httpResp = response as? HTTPURLResponse,
              (httpResp.statusCode == 200 || httpResp.statusCode == 206) else {
            throw URLError(.badServerResponse)
        }

        // 移动到目标路径
        let destDir = modelFile.deletingLastPathComponent()
        try? FileManager.default.createDirectory(at: destDir, withIntermediateDirectories: true)
        if FileManager.default.fileExists(atPath: modelFile.path) {
            try FileManager.default.removeItem(at: modelFile)
        }
        try FileManager.default.moveItem(at: tempURL, to: modelFile)
    }

    /// 加载模型到内存
    private func loadModel() {
        guard modelFile.exists() else { return }
        isLoadingModel = true

        DispatchQueue.global(qos: .userInitiated).async { [weak self] in
            guard let self = self else { return }
            do {
                let modelPath = self.modelFile.path
                self.model = try LLModel(path: modelPath)
                DispatchQueue.main.async {
                    self.isLoadingModel = false
                    self.isModelReady = true
                }
            } catch {
                DispatchQueue.main.async {
                    self.isLoadingModel = false
                    self.downloadError = "模型加载失败: \(error.localizedDescription)"
                }
            }
        }
    }

    /// 生成回复
    func generate(prompt: String, maxTokens: Int = 512, temperature: Float = 0.7) async throws -> String {
        guard let model = model else {
            throw NSError(domain: "CunyiAI", code: -1, userInfo: [NSLocalizedDescriptionKey: "模型未加载"])
        }

        return try await withCheckedThrowingContinuation { continuation in
            queue.async {
                do {
                    let result = try model.predict(
                        prompt,
                        maxTokenCount: maxTokens,
                        temperature: temperature,
                        repeatPenalty: 1.1,
                        repeatLastN: 64
                    )
                    continuation.resume(returning: result)
                } catch {
                    continuation.resume(throwing: error)
                }
            }
        }
    }
}
