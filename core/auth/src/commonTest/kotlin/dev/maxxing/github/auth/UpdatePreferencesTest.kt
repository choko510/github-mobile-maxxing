package dev.maxxing.github.auth

import kotlinx.serialization.encodeToString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UpdatePreferencesTest {
    private val json = TokenStoreJson

    @Test
    fun defaultPreferencesFollowCurrentChannel() {
        val prefs = UpdatePreferences()
        assertNull(prefs.includePrereleases)
        assertTrue(prefs.autoCheckOnLaunch)
        assertNull(prefs.dismissedVersionKey)

        // Stable build defaults to prereleases OFF; nightly build defaults to prereleases ON.
        assertFalse(prefs.resolveIncludePrereleases(isCurrentBuildNightly = false))
        assertTrue(prefs.resolveIncludePrereleases(isCurrentBuildNightly = true))
    }

    @Test
    fun explicitPrereleaseChoiceOverridesChannelDefaultAndRoundTrips() {
        val disabledOnNightly = UpdatePreferences(
            includePrereleases = false,
            autoCheckOnLaunch = false,
            dismissedVersionKey = "nightly:v1.0.90-nightly.abc1234"
        )
        assertFalse(disabledOnNightly.resolveIncludePrereleases(isCurrentBuildNightly = true))

        val encoded = json.encodeToString(disabledOnNightly)
        val decoded = json.decodeFromString<UpdatePreferences>(encoded)
        assertEquals(disabledOnNightly, decoded)
    }
}
