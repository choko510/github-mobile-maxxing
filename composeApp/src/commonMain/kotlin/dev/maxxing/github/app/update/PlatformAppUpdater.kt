package dev.maxxing.github.app.update

expect object PlatformAppUpdater {
    val platformKind: UpdatePlatformKind
    val supportsInAppInstall: Boolean

    fun canInstallPackages(): Boolean
    fun openInstallUnknownAppsSettings(): Boolean

    suspend fun downloadUpdateAsset(
        url: String,
        fileName: String,
        expectedSizeBytes: Long?,
        onProgress: (downloadedBytes: Long, totalBytes: Long?) -> Unit
    ): Result<String>

    fun installDownloadedPackage(filePath: String): Boolean
}
