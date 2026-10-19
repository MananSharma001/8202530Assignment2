package com.s8202530.assignment2.data

import com.s8202530.assignment2.data.model.AppSummary
import com.s8202530.assignment2.data.model.PermissionGroup
import com.s8202530.assignment2.data.model.PermissionStats
import org.junit.Assert.assertEquals
import org.junit.Test

class PermissionStatsTest {

    @Test
    fun countsGrantedPermissionsAndSensitiveApps() {
        val apps = listOf(
            app("Maps", granted = 3, requested = 4, PermissionGroup.LOCATION, PermissionGroup.CAMERA),
            app("Chat", granted = 2, requested = 5, PermissionGroup.CAMERA, PermissionGroup.MICROPHONE),
            app("Notes", granted = 0, requested = 1),
        )

        val stats = PermissionStats.from(apps)

        assertEquals(3, stats.appCount)
        assertEquals(5, stats.grantedTotal)
        assertEquals(10, stats.requestedTotal)
        assertEquals(50, stats.grantedPercent)
        assertEquals(1, stats.locationApps)
        assertEquals(2, stats.cameraApps)
        assertEquals(1, stats.microphoneApps)
    }

    @Test
    fun noApps_givesZeroPercentInsteadOfCrashing() {
        assertEquals(0, PermissionStats.from(emptyList()).grantedPercent)
    }

    @Test
    fun permissionsAreGroupedLikeSettings() {
        assertEquals(PermissionGroup.LOCATION, PermissionGroup.of("android.permission.ACCESS_FINE_LOCATION"))
        assertEquals(PermissionGroup.MICROPHONE, PermissionGroup.of("android.permission.RECORD_AUDIO"))
        assertEquals(PermissionGroup.FILES_AND_MEDIA, PermissionGroup.of("android.permission.READ_MEDIA_IMAGES"))
        assertEquals(PermissionGroup.OTHER, PermissionGroup.of("com.example.permission.CUSTOM"))
    }

    private fun app(label: String, granted: Int, requested: Int, vararg groups: PermissionGroup) =
        AppSummary(
            packageName = "com.example.${label.lowercase()}",
            label = label,
            isSystem = false,
            grantedCount = granted,
            requestedCount = requested,
            grantedGroups = groups.toSet(),
        )
}
