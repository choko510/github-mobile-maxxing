package dev.maxxing.github.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.composables.icons.lucide.Download
import com.composables.icons.lucide.ExternalLink
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.ShieldAlert
import com.composables.icons.lucide.Sparkles
import dev.maxxing.github.app.AppContainer
import dev.maxxing.github.app.AppUpdateCheckState
import dev.maxxing.github.app.AppUpdateInstallState
import dev.maxxing.github.app.MaxxingBuildConfig
import dev.maxxing.github.app.update.PlatformAppUpdater
import dev.maxxing.github.ui.components.ExpressiveStatusPill
import dev.maxxing.github.ui.components.GitHubMarkdown
import dev.maxxing.github.ui.components.MaxxingWavyProgress
import dev.maxxing.github.ui.i18n.appStrings

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun AppUpdateBottomSheet(
    container: AppContainer,
    onMessage: (String) -> Unit
) {
    val showSheet by container.showUpdateSheet.collectAsState()
    val checkState by container.updateCheckState.collectAsState()
    val installState by container.updateInstallState.collectAsState()
    val available = (checkState as? AppUpdateCheckState.Available)?.update
    if (!showSheet || available == null) return

    val strings = appStrings()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, installState) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && installState is AppUpdateInstallState.PermissionRequired) {
                container.retryPendingInstallIfPermitted(strings.appUpdateFailedOpenInstaller)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isDownloading = installState is AppUpdateInstallState.Downloading
    val currentVersionLabel = "v${MaxxingBuildConfig.APP_VERSION_NAME.trim().removePrefix("v").ifBlank { "dev" }}"

    ModalBottomSheet(
        onDismissRequest = {
            container.dismissUpdateSheet(rememberDismissal = !isDownloading)
        },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Lucide.Sparkles,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = strings.appUpdateSheetTitle,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = strings.appUpdateVersionTransition(currentVersionLabel, available.versionLabel),
                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                ExpressiveStatusPill(
                    label = if (available.isPrerelease) {
                        strings.appUpdateChannelNightly
                    } else {
                        strings.appUpdateChannelStable
                    },
                    containerColor = if (available.isPrerelease) {
                        MaterialTheme.colorScheme.tertiaryContainer
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    },
                    contentColor = if (available.isPrerelease) {
                        MaterialTheme.colorScheme.onTertiaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    }
                )
            }

            val sizeText = available.asset.sizeBytes.takeIf { it > 0L }?.let(::formatUpdateBytes)
            val assetInfo = listOfNotNull(available.asset.name, sizeText).joinToString(" · ")
            Text(
                text = assetInfo,
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (available.release.body.isNotBlank()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = strings.appUpdateReleaseNotesTitle,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 260.dp)
                                .verticalScroll(rememberScrollState())
                                .padding(14.dp)
                        ) {
                            GitHubMarkdown(
                                content = available.release.body,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            when (val state = installState) {
                is AppUpdateInstallState.Downloading -> {
                    val total = state.totalBytes?.takeIf { it > 0L }
                    val progress = total?.let { (state.downloadedBytes.toFloat() / it.toFloat()).coerceIn(0f, 1f) }
                    val pctText = progress?.let { " (${(it * 100).toInt()}%)" }.orEmpty()
                    val statusDetail = if (total != null) {
                        "${formatUpdateBytes(state.downloadedBytes)} / ${formatUpdateBytes(total)}$pctText"
                    } else {
                        formatUpdateBytes(state.downloadedBytes)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = strings.appUpdateDownloading(statusDetail),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        MaxxingWavyProgress(progress = progress)
                    }
                }

                is AppUpdateInstallState.PermissionRequired -> {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Lucide.ShieldAlert,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = strings.appUpdatePermissionRequired,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                is AppUpdateInstallState.Error -> {
                    Text(
                        text = state.detail?.takeIf { it.isNotBlank() } ?: strings.appUpdateFailedDownload,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                AppUpdateInstallState.Idle,
                is AppUpdateInstallState.ReadyToInstall -> Unit
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (installState) {
                    is AppUpdateInstallState.Downloading -> {
                        OutlinedButton(onClick = { container.cancelUpdateDownload() }) {
                            Text(strings.cancel)
                        }
                    }

                    is AppUpdateInstallState.PermissionRequired -> {
                        TextButton(onClick = { container.dismissUpdateSheet(rememberDismissal = true) }) {
                            Text(strings.appUpdateLater)
                        }
                        Button(onClick = { PlatformAppUpdater.openInstallUnknownAppsSettings() }) {
                            Text(strings.appUpdateOpenInstallSettings)
                        }
                    }

                    is AppUpdateInstallState.ReadyToInstall -> {
                        TextButton(onClick = { container.dismissUpdateSheet(rememberDismissal = true) }) {
                            Text(strings.appUpdateLater)
                        }
                        Button(onClick = { container.installPendingPackage(strings.appUpdateFailedOpenInstaller) }) {
                            Icon(Lucide.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(strings.appUpdateInstallNow)
                        }
                    }

                    AppUpdateInstallState.Idle,
                    is AppUpdateInstallState.Error -> {
                        TextButton(onClick = { container.dismissUpdateSheet(rememberDismissal = true) }) {
                            Text(strings.appUpdateLater)
                        }
                        Button(
                            onClick = {
                                container.startUpdateAction(
                                    onMessage = onMessage,
                                    browserFailedMessage = strings.appUpdateFailedOpenBrowser,
                                    installerFailedMessage = strings.appUpdateFailedOpenInstaller
                                )
                            }
                        ) {
                            Icon(
                                imageVector = if (PlatformAppUpdater.supportsInAppInstall) Lucide.Download else Lucide.ExternalLink,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                if (PlatformAppUpdater.supportsInAppInstall) {
                                    strings.appUpdateDownloadAndInstall
                                } else {
                                    strings.appUpdateDownloadInBrowser
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

internal fun formatUpdateBytes(bytes: Long): String {
    if (bytes < 1024L) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024.0) return "${formatOneDecimal(kb)} KB"
    val mb = kb / 1024.0
    return "${formatOneDecimal(mb)} MB"
}

private fun formatOneDecimal(value: Double): String {
    val tenths = ((value * 10.0) + 0.5).toLong()
    val whole = tenths / 10L
    val frac = tenths % 10L
    return "$whole.$frac"
}
