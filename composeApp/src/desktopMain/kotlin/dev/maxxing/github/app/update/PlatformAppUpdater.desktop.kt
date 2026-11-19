package dev.maxxing.github.app.update

actual object PlatformAppUpdater {
    actual val platformKind: UpdatePlatformKind = UpdatePlatformKind.DESKTOP
    actual val supportsInAppInstall: Boolean = false

    actual fun canInstallPackages(): Boolean = true

    actual fun openInstallUnknownAppsSettings(): Boolean = false

    actual suspend fun downloadUpdateAsset(
        url: String,
        fileName: String,
        expectedSizeBytes: Long?,
        onProgress: (downloadedBytes: Long, totalBytes: Long?) -> Unit
    ): Result<String> = Result.failure(
        UnsupportedOperationException("In-app binary installation is not supported on Desktop.")
    )

    actual fun installDownloadedPackage(filePath: String): Boolean = false
}
