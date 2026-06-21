package com.runcheck.util

import android.Manifest
import android.os.Build
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RuncheckPermissionPolicyTest {
    @Test
    fun `wifi detail permission request includes coarse and fine location together`() {
        assertEquals(
            listOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ),
            RuncheckPermissionPolicy.wifiDetailLocationPermissions,
        )
    }

    @Test
    fun `android 14 media request includes selected visual media permission`() {
        assertEquals(
            listOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
            ),
            RuncheckPermissionPolicy.mediaPermissionsForApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE),
        )
    }

    @Test
    fun `android 13 media request uses granular media permissions without selected visual media`() {
        assertEquals(
            listOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO,
            ),
            RuncheckPermissionPolicy.mediaPermissionsForApi(Build.VERSION_CODES.TIRAMISU),
        )
    }

    @Test
    fun `android 14 selected visual media without full grants is partial access`() {
        val granted =
            setOf(
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
                Manifest.permission.READ_MEDIA_AUDIO,
            )

        assertEquals(
            MediaAccessState.PARTIAL_VISUAL,
            RuncheckPermissionPolicy.mediaAccessStateForApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                it in granted
            },
        )
    }

    @Test
    fun `android 14 all media grants are full access`() {
        val granted =
            setOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO,
            )

        assertEquals(
            MediaAccessState.FULL,
            RuncheckPermissionPolicy.mediaAccessStateForApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                it in granted
            },
        )
    }

    @Test
    fun `notification runtime permission is only required from android 13`() {
        assertFalse(RuncheckPermissionPolicy.isNotificationRuntimePermissionRequired(Build.VERSION_CODES.S_V2))
        assertTrue(RuncheckPermissionPolicy.isNotificationRuntimePermissionRequired(Build.VERSION_CODES.TIRAMISU))
    }
}
