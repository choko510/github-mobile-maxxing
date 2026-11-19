package dev.maxxing.github.app.update

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import dev.maxxing.github.auth.AndroidAuthContextHolder
import dev.maxxing.github.network.downloadBinaryAsset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

private const val UPDATES_CACHE_DIR = "updates"

actual object PlatformAppUpdater {
    actual val platformKind: UpdatePlatformKind = UpdatePlatformKind.ANDROID
    actual val supportsInAppInstall: Boolean = true

    actual fun canInstallPackages(): Boolean {
        val context = runCatching { AndroidAuthContextHolder.requireContext() }.getOrNull() ?: return false
        return runCatching { context.packageManager.canRequestPackageInstalls() }.getOrDefault(false)
    }

    actual fun openInstallUnknownAppsSettings(): Boolean {
        val context = runCatching { AndroidAuthContextHolder.requireContext() }.getOrNull() ?: return false
        return runCatching {
            val intent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }

    actual suspend fun downloadUpdateAsset(
        url: String,
        fileName: String,
        expectedSizeBytes: Long?,
        onProgress: (downloadedBytes: Long, totalBytes: Long?) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val context = AndroidAuthContextHolder.requireContext()
            val updatesDir = File(context.cacheDir, UPDATES_CACHE_DIR).apply { mkdirs() }
            // Remove stale APKs / partial downloads from earlier updates before writing a new one.
            updatesDir.listFiles()?.forEach { old ->
                if (old.isFile) old.delete()
            }

            val safeName = fileName.substringAfterLast('/').ifBlank { "update.apk" }
            val tempFile = File(updatesDir, "$safeName.tmp")
            val finalFile = File(updatesDir, safeName)

            try {
                FileOutputStream(tempFile).use { output ->
                    downloadBinaryAsset(
                        url = url,
                        expectedSizeBytes = expectedSizeBytes
                    ) { bytes, length, downloaded, total ->
                        output.write(bytes, 0, length)
                        onProgress(downloaded, total)
                    }
                    output.flush()
                }
                if (!hasZipSignature(tempFile)) {
                    throw IllegalStateException("Downloaded file is not a valid APK archive")
                }
                if (finalFile.exists()) finalFile.delete()
                if (!tempFile.renameTo(finalFile)) {
                    tempFile.copyTo(finalFile, overwrite = true)
                    tempFile.delete()
                }
                finalFile.absolutePath
            } catch (t: Throwable) {
                tempFile.delete()
                throw t
            }
        }
    }

    actual fun installDownloadedPackage(filePath: String): Boolean {
        val context = runCatching { AndroidAuthContextHolder.requireContext() }.getOrNull() ?: return false
        val apkFile = File(filePath)
        if (!apkFile.exists() || !apkFile.isFile) return false
        return runCatching {
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }

    private fun hasZipSignature(file: File): Boolean {
        if (file.length() < 4) return false
        return runCatching {
            file.inputStream().use { input ->
                val b0 = input.read()
                val b1 = input.read()
                b0 == 0x50 && b1 == 0x4B // 'P', 'K'
            }
        }.getOrDefault(false)
    }
}
