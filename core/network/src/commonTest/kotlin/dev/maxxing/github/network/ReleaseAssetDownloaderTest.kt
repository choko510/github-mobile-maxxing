package dev.maxxing.github.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class ReleaseAssetDownloaderTest {

    @Test
    fun streamsBytesReportsProgressAndOmitsAuthorization() = runTest {
        val payload = ByteArray(100_000) { (it % 251).toByte() }
        var seenAuthHeader: String? = null
        val engine = MockEngine { request ->
            seenAuthHeader = request.headers[HttpHeaders.Authorization]
            respond(
                content = payload,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentLength, payload.size.toString())
            )
        }
        val client = HttpClient(engine)
        val collected = mutableListOf<Byte>()
        val progressEvents = mutableListOf<Pair<Long, Long?>>()

        downloadBinaryAsset(
            url = "https://objects.githubusercontent.com/assets/app-release.apk",
            httpClient = client
        ) { bytes, length, downloaded, total ->
            for (i in 0 until length) {
                collected.add(bytes[i])
            }
            progressEvents.add(downloaded to total)
        }

        assertNull(seenAuthHeader)
        assertContentEquals(payload, collected.toByteArray())
        assertEquals(payload.size.toLong() to payload.size.toLong(), progressEvents.last())
        client.close()
    }

    @Test
    fun throwsGitHubApiExceptionOnNonSuccessStatus() = runTest {
        val engine = MockEngine {
            respond(
                content = "Not Found",
                status = HttpStatusCode.NotFound
            )
        }
        val client = HttpClient(engine)
        val err = assertFailsWith<GitHubApiException> {
            downloadBinaryAsset(
                url = "https://github.com/neutron42-jp/github-mobile-maxxing/releases/download/v1.0.0/missing.apk",
                httpClient = client
            ) { _, _, _, _ -> }
        }
        assertEquals(404, err.statusCode)
        client.close()
    }
}
