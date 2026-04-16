package com.cunyi.ai.model

import android.content.Context
import com.cunyi.ai.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 村医AI - LiteRT引擎封装
 * 首次启动自动下载 GGUF 模型，然后通过 JNI 调用 LiteRT-LM C++ 运行时
 */
class LiteRTEngine(private val context: Context) {

    private var isReady = false
    private var isLoading = false

    val modelUrl: String = BuildConfig.MODEL_URL
    val modelFileName: String = "gemma-4-E2B-it-Q4_K_M.gguf"

    val modelFile: java.io.File
        get() = java.io.File(context.filesDir, "models/$modelFileName")

    val isModelReady: Boolean
        get() = isReady && !isLoading && modelFile.exists()

    suspend fun initialize(): Result<Unit> = withContext(Dispatchers.IO) {
        if (isLoading) return@withContext Result.failure(Exception("Already loading"))
        if (isReady && modelFile.exists()) {
            isReady = true
            return@withContext Result.success(Unit)
        }

        isLoading = true
        try {
            // 1. 确保模型文件存在（下载或使用本地）
            if (!modelFile.exists()) {
                modelFile.parentFile?.mkdirs()
                downloadModel()
            }

            // 2. 加载模型到内存
            val loaded = loadModelNative(modelFile.absolutePath)
            if (loaded) {
                isReady = true
                Result.success(Unit)
            } else {
                Result.failure(Exception("Native model loading failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            isLoading = false
        }
    }

    private suspend fun downloadModel() {
        val downloader = ModelDownloader(context, modelUrl, modelFile)
        downloader.download { progress ->
            // UI 回调：progress 0.0 ~ 1.0
        }
    }

    private external fun loadModelNative(path: String): Boolean

    suspend fun generate(prompt: String, maxTokens: Int = 512, temperature: Float = 0.7f): Result<String> =
        withContext(Dispatchers.IO) {
            if (!isReady) return@withContext Result.failure(Exception("Model not ready"))
            try {
                val result = generateNative(prompt, maxTokens, temperature)
                Result.success(result)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private external fun generateNative(prompt: String, maxTokens: Int, temperature: Float): String

    fun release() {
        if (isReady) {
            unloadModelNative()
            isReady = false
        }
    }

    private external fun unloadModelNative()

    companion object {
        init {
            System.loadLibrary("cunyi_litert_jni")
        }
    }
}
