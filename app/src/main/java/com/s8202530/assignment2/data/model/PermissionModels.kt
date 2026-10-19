package com.s8202530.assignment2.data.model

import android.os.Build
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.s8202530.assignment2.R

/** Groups permissions the same way Android's Settings app does. */
enum class PermissionGroup(@StringRes val label: Int, @DrawableRes val icon: Int) {
    LOCATION(R.string.group_location, R.drawable.ic_location),
    CAMERA(R.string.group_camera, R.drawable.ic_camera),
    MICROPHONE(R.string.group_microphone, R.drawable.ic_mic),
    CONTACTS(R.string.group_contacts, R.drawable.ic_person),
    PHONE(R.string.group_phone, R.drawable.ic_info),
    SMS(R.string.group_sms, R.drawable.ic_info),
    FILES_AND_MEDIA(R.string.group_files, R.drawable.ic_info),
    CALENDAR(R.string.group_calendar, R.drawable.ic_info),
    BODY_SENSORS(R.string.group_body_sensors, R.drawable.ic_info),
    PHYSICAL_ACTIVITY(R.string.group_activity, R.drawable.ic_info),
    NEARBY_DEVICES(R.string.group_nearby, R.drawable.ic_info),
    NOTIFICATIONS(R.string.group_notifications, R.drawable.ic_info),
    OTHER(R.string.group_other, R.drawable.ic_key);

    companion object {
        private const val P = "android.permission."

        private val byPermission: Map<String, PermissionGroup> = buildMap {
            listOf("ACCESS_FINE_LOCATION", "ACCESS_COARSE_LOCATION", "ACCESS_BACKGROUND_LOCATION")
                .forEach { put(P + it, LOCATION) }
            put(P + "CAMERA", CAMERA)
            put(P + "RECORD_AUDIO", MICROPHONE)
            listOf("READ_CONTACTS", "WRITE_CONTACTS", "GET_ACCOUNTS")
                .forEach { put(P + it, CONTACTS) }
            listOf(
                "READ_PHONE_STATE", "READ_PHONE_NUMBERS", "CALL_PHONE", "ANSWER_PHONE_CALLS",
                "READ_CALL_LOG", "WRITE_CALL_LOG", "ADD_VOICEMAIL", "USE_SIP",
                "PROCESS_OUTGOING_CALLS", "ACCEPT_HANDOVER",
            ).forEach { put(P + it, PHONE) }
            listOf("SEND_SMS", "RECEIVE_SMS", "READ_SMS", "RECEIVE_WAP_PUSH", "RECEIVE_MMS")
                .forEach { put(P + it, SMS) }
            listOf(
                "READ_EXTERNAL_STORAGE", "WRITE_EXTERNAL_STORAGE", "ACCESS_MEDIA_LOCATION",
                "READ_MEDIA_IMAGES", "READ_MEDIA_VIDEO", "READ_MEDIA_AUDIO",
                "READ_MEDIA_VISUAL_USER_SELECTED",
            ).forEach { put(P + it, FILES_AND_MEDIA) }
            listOf("READ_CALENDAR", "WRITE_CALENDAR").forEach { put(P + it, CALENDAR) }
            listOf("BODY_SENSORS", "BODY_SENSORS_BACKGROUND").forEach { put(P + it, BODY_SENSORS) }
            put(P + "ACTIVITY_RECOGNITION", PHYSICAL_ACTIVITY)
            listOf(
                "BLUETOOTH_SCAN", "BLUETOOTH_CONNECT", "BLUETOOTH_ADVERTISE",
                "NEARBY_WIFI_DEVICES", "UWB_RANGING",
            ).forEach { put(P + it, NEARBY_DEVICES) }
            put(P + "POST_NOTIFICATIONS", NOTIFICATIONS)
        }

        fun of(permission: String): PermissionGroup = byPermission[permission] ?: OTHER
    }
}

/** A runtime ("dangerous") permission an app has asked for. */
data class AppPermission(
    val name: String,   // e.g. android.permission.CAMERA
    val label: String,  // e.g. "Take pictures and videos"
    val group: PermissionGroup,
    val granted: Boolean,
)

/** One row in the App permissions list. */
data class AppSummary(
    val packageName: String,
    val label: String,
    val isSystem: Boolean,
    val grantedCount: Int,
    val requestedCount: Int,
    val grantedGroups: Set<PermissionGroup>,
)

/** Everything the App detail screen shows. */
data class AppDetail(
    val packageName: String,
    val label: String,
    val versionName: String?,
    val targetSdk: Int,
    val isSystem: Boolean,
    val permissions: List<AppPermission>,
) {
    val grantedCount: Int get() = permissions.count { it.granted }

    /** Apps built for Android 5.1 or older get every permission at install time. */
    val isLegacy: Boolean get() = targetSdk < Build.VERSION_CODES.M
}

/** The numbers in the summary card at the top of App permissions. */
data class PermissionStats(
    val appCount: Int,
    val grantedTotal: Int,
    val requestedTotal: Int,
    val locationApps: Int,
    val cameraApps: Int,
    val microphoneApps: Int,
) {
    val grantedPercent: Int
        get() = if (requestedTotal == 0) 0 else (grantedTotal * 100) / requestedTotal

    companion object {
        fun from(apps: List<AppSummary>) = PermissionStats(
            appCount = apps.size,
            grantedTotal = apps.sumOf { it.grantedCount },
            requestedTotal = apps.sumOf { it.requestedCount },
            locationApps = apps.count { PermissionGroup.LOCATION in it.grantedGroups },
            cameraApps = apps.count { PermissionGroup.CAMERA in it.grantedGroups },
            microphoneApps = apps.count { PermissionGroup.MICROPHONE in it.grantedGroups },
        )
    }
}
