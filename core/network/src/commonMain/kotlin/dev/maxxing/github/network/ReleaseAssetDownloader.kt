package dev.maxxing.github.network

import dev.maxxing.github.model.AppBranding
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.contentLength
import io.ktor.http.isSuccess
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/**
 * Streams a binary release asset from [url] without attaching GitHub API Authorization headers
 * (GitHub release asset URLs redirect to CDN/S3 signed URLs that reject bearer tokens).
 */
suspend fun downloadBinaryAsset(
    url: String,
    httpClient: HttpClient? = null,
    expectedSizeBytes: Long? = null,
    onChunk: suspend (bytes: ByteArray, length: Int, downloadedBytes: Long, totalBytes: Long?) -> Unit
) {
    val ownedClient = httpClient == null
    val client = httpClient ?: HttpClient()
    try {
        client.prepareGet(url) {
            header(HttpHeaders.UserAgent, AppBranding.USER_AGENT)
        }.execute { response ->
            if (!response.status.isSuccess()) {
                throw GitHubApiException(
                    response.status.value,
                    "Failed to download update (${response.status.value})"
                )
            }
            val totalBytes = response.contentLength()?.takeIf { it > 0L }
                ?: expectedSizeBytes?.takeIf { it > 0L }
            val channel = response.bodyAsChannel()
            val buffer = ByteArray(64 * 1024)
            var downloaded = 0L
            while (true) {
                currentCoroutineContext().ensureActive()
                val read = channel.readAvailable(buffer, 0, buffer.size)
                if (read < 0) break
                if (read == 0) continue
                downloaded += read
                onChunk(buffer, read, downloaded, totalBytes)
            }
            if (downloaded == 0L) {
                throw IllegalStateException("Downloaded update file is empty")
            }
        }
    } finally {
        if (ownedClient) {
            client.close()
        }
    }
}
