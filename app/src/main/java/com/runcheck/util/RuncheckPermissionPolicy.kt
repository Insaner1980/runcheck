package com.runcheck.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

enum class MediaAccessState {
    FULL,
    PARTIAL_VISUAL,
    MISSING,
}

object RuncheckPermissionPolicy {
    val wifiDetailLocationPermissions: List<String> =
        listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )

    fun mediaPermissionsForApi(apiLevel: Int = Build.VERSION.SDK_INT): List<String> =
        when {
            apiLevel >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
                listOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.READ_MEDIA_AUDIO,
                    Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
                )
            }

            apiLevel >= Build.VERSION_CODES.TIRAMISU -> {
                listOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.READ_MEDIA_AUDIO,
                )
            }

            apiLevel >= Build.VERSION_CODES.Q -> {
                listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            }

            else -> {
                listOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                )
            }
        }

    fun mediaAccessStateForApi(
        apiLevel: Int = Build.VERSION.SDK_INT,
        isGranted: (String) -> Boolean,
    ): MediaAccessState {
        val permissions = mediaPermissionsForApi(apiLevel)
        if (apiLevel < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return if (permissions.all(isGranted)) MediaAccessState.FULL else MediaAccessState.MISSING
        }

        val hasFullAccess =
            isGranted(Manifest.permission.READ_MEDIA_IMAGES) ||
                isGranted(Manifest.permission.READ_MEDIA_VIDEO)
        if (hasFullAccess) return MediaAccessState.FULL

        return if (isGranted(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)) {
            MediaAccessState.PARTIAL_VISUAL
        } else {
            MediaAccessState.MISSING
        }
    }

    fun shouldOpenMediaSettings(
        permissionRequested: Boolean,
        mediaAccessState: MediaAccessState,
        missingPermissions: List<String>,
        shouldShowRationale: (String) -> Boolean,
    ): Boolean =
        permissionRequested &&
            mediaAccessState != MediaAccessState.PARTIAL_VISUAL &&
            missingPermissions.isNotEmpty() &&
            missingPermissions.none(shouldShowRationale)

    fun isNotificationRuntimePermissionRequired(apiLevel: Int = Build.VERSION.SDK_INT): Boolean =
        apiLevel >= Build.VERSION_CODES.TIRAMISU

    fun canPostNotifications(context: Context): Boolean =
        !isNotificationRuntimePermissionRequired() ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
}
