package com.cunyi.ai.model

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

/**
 * GGUF 模型下载器
 * 支持断点续传，进度回调
 */
class ModelDownloader(
    private val context: Context,
    private val url: String,
    private val destFile: File
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS) // 无超时，用于大文件
        .build()

    sealed class DownloadState {
        data object Idle : DownloadState()
        data class Downloading(val progress: Float, val downloadedMB: Float, val totalMB: Float) : DownloadState()
        data object Completed : DownloadState()
        data class Error(val msg: String) : DownloadState()
    }

    suspend fun download(onProgress: (Float) -> Unit): Result<File> = withContext(Dispatchers.IO) {
        try {
            // 先发 HEAD 请求获取文件大小
            val headRequest = Request.Builder().url(url).head().build()
            val headResponse = client.newCall(headRequest).execute()
            val contentLength = headResponse.header("Content-Length")?.toLongOrNull() ?: -1L
            headResponse.close()

            val existingBytes = if (destFile.exists()) destFile.length() else 0L
            val resumeFrom = if (existingBytes > 0 && existingBytes < (contentLength.takeIf { it > 0 } ?: Long.MAX_VALUE)) {
                existingBytes
            } else {
                // 文件损坏或不存在，重新下载
                destFile.delete()
                0L
            }

            val requestBuilder = Request.Builder().url(url)
            if (resumeFrom > 0) {
                requestBuilder.addHeader("Range", "bytes=$resumeFrom-")
            }

            val response: Response = client.newCall(requestBuilder.build()).execute()

            if (!response.isSuccessful && response.code != 206) {
                return@withContext Result.failure(Exception("HTTP ${response.code}"))
            }

            val body = response.body ?: return@withContext Result.failure(Exception("Empty response"))
            val totalBytes = if (contentLength > 0) contentLength else body.contentLength()
            val inputStream = body.byteStream()
            val outputStream = FileOutputStream(destFile, resumeFrom > 0)

            val buffer = ByteArray(8192)
            var bytesRead: Int
            var totalRead = resumeFrom
            var lastReportedProgress = -1f

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalRead += bytesRead

                if (totalBytes > 0) {
                    val progress = (totalRead.toFloat() / totalBytes).coerceIn(0f, 1f)
                    if (progress - lastReportedProgress >= 0.005f) { // 每 0.5% 回调一次
                        lastReportedProgress = progress
                        onProgress(progress)
                    }
                }
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()

            onProgress(1f)
            Result.success(destFile)

        } catch (e: Exception) {
            destFile.delete()
            Result.failure(e)
        }
    }
}
