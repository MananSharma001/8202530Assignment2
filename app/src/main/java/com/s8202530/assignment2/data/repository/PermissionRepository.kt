package com.s8202530.assignment2.data.repository

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import android.os.Build
import com.s8202530.assignment2.data.model.AppDetail
import com.s8202530.assignment2.data.model.AppPermission
import com.s8202530.assignment2.data.model.AppSummary
import com.s8202530.assignment2.data.model.PermissionGroup
import com.s8202530.assignment2.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

/** Reads installed apps and their runtime permissions. An interface so tests can mock it. */
interface PermissionRepository {
    /** Every app that asks for at least one runtime permission, A to Z. */
    suspend fun loadApps(): List<AppSummary>

    /** Fresh permission state for one app, or null if it's been uninstalled. */
    suspend fun loadApp(packageName: String): AppDetail?
}

/** Reads straight from Android's PackageManager. Reading needs no special access. */
class PermissionRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context,
    @IoDispatcher private val io: CoroutineDispatcher,
) : PermissionRepository {

    private val pm: PackageManager = context.packageManager

    private data class PermissionMeta(val isRuntime: Boolean, val label: String)

    private val metaCache = ConcurrentHashMap<String, PermissionMeta>()

    override suspend fun loadApps(): List<AppSummary> = withContext(io) {
        installedPackages().mapNotNull { info ->
            val app = info.applicationInfo ?: return@mapNotNull null
            val permissions = runtimePermissions(info)
            if (permissions.isEmpty()) return@mapNotNull null
            AppSummary(
                packageName = info.packageName,
                label = app.loadLabel(pm).toString(),
                isSystem = app.isSystemApp(),
                grantedCount = permissions.count { it.granted },
                requestedCount = permissions.size,
                grantedGroups = permissions.filter { it.granted }.map { it.group }.toSet(),
            )
        }.sortedBy { it.label.lowercase() }
    }

    override suspend fun loadApp(packageName: String): AppDetail? = withContext(io) {
        val info = try {
            packageInfo(packageName)
        } catch (e: PackageManager.NameNotFoundException) {
            return@withContext null
        }
        val app = info.applicationInfo ?: return@withContext null
        AppDetail(
            packageName = info.packageName,
            label = app.loadLabel(pm).toString(),
            versionName = info.versionName,
            targetSdk = app.targetSdkVersion,
            isSystem = app.isSystemApp(),
            permissions = runtimePermissions(info),
        )
    }

    private fun runtimePermissions(info: PackageInfo): List<AppPermission> {
        val names = info.requestedPermissions ?: return emptyList()
        val flags = info.requestedPermissionsFlags
        return names.mapIndexedNotNull { index, name ->
            val meta = meta(name)
            if (!meta.isRuntime) return@mapIndexedNotNull null
            val granted = flags != null && index < flags.size &&
                (flags[index] and PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0
            AppPermission(
                name = name,
                label = meta.label,
                group = PermissionGroup.of(name),
                granted = granted,
            )
        }
            .distinctBy { it.name }
            .sortedWith(compareBy({ it.group.ordinal }, { it.label.lowercase() }))
    }

    private fun meta(name: String): PermissionMeta = metaCache.getOrPut(name) {
        try {
            val info = pm.getPermissionInfo(name, 0)
            PermissionMeta(
                isRuntime = info.baseProtection() == PermissionInfo.PROTECTION_DANGEROUS,
                label = prettyLabel(info.loadLabel(pm).toString(), name),
            )
        } catch (e: PackageManager.NameNotFoundException) {
            PermissionMeta(isRuntime = false, label = name)
        }
    }

    /** "dangerous" means a runtime permission the user can allow or deny. */
    @Suppress("DEPRECATION")
    private fun PermissionInfo.baseProtection(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) protection
        else protectionLevel and PermissionInfo.PROTECTION_MASK_BASE

    private fun prettyLabel(label: String, name: String): String {
        val text = if (label.isBlank() || label == name) {
            name.substringAfterLast('.').replace('_', ' ').lowercase()
        } else {
            label
        }
        return text.replaceFirstChar { it.uppercaseChar() }
    }

    @Suppress("DEPRECATION")
    private fun installedPackages(): List<PackageInfo> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getInstalledPackages(
                PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong())
            )
        } else {
            pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)
        }

    @Suppress("DEPRECATION")
    private fun packageInfo(packageName: String): PackageInfo =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageInfo(
                packageName,
                PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong())
            )
        } else {
            pm.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
        }

    private fun ApplicationInfo.isSystemApp(): Boolean =
        (flags and ApplicationInfo.FLAG_SYSTEM) != 0
}
