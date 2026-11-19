package dev.maxxing.github.app.update

actual object PlatformAppUpdater {
    actual val platformKind: UpdatePlatformKind = UpdatePlatformKind.IOS
    actual val supportsInAppInstall: Boolean = false

    actual fun canInstallPackages(): Boolean = false

    actual fun openInstallUnknownAppsSettings(): Boolean = false

    actual suspend fun downloadUpdateAsset(
        url: String,
        fileName: String,
        expectedSizeBytes: Long?,
        onProgress: (downloadedBytes: Long, totalBytes: Long?) -> Unit
    ): Result<String> = Result.failure(
        UnsupportedOperationException("In-app updates are not supported on iOS.")
    )

    actual fun installDownloadedPackage(filePath: String): Boolean = false
}
