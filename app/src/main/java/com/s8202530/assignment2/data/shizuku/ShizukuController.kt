package com.s8202530.assignment2.data.shizuku

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import com.s8202530.assignment2.BuildConfig
import com.s8202530.assignment2.IUserService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import rikka.shizuku.Shizuku
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Where the Shizuku bridge is in its setup, for the UI to react to. */
enum class ShizukuStatus {
    NOT_INSTALLED,   // the Shizuku app isn't installed
    NOT_RUNNING,     // installed, but the service hasn't been started via ADB
    NEEDS_PERMISSION,// running, but this app hasn't been authorised yet
    READY,           // good to grant/revoke
}

/** Grants and revokes runtime permissions. An interface so tests can mock it. */
interface PermissionController {
    val status: StateFlow<ShizukuStatus>
    fun refreshStatus()
    fun requestPermission()
    suspend fun grant(packageName: String, permission: String): Result<Unit>
    suspend fun revoke(packageName: String, permission: String): Result<Unit>
}

/**
 * The bridge that lets this app run privileged `pm grant` / `pm revoke`
 * commands through Shizuku, without root. Nothing here changes a permission on
 * its own; it only forwards shell commands to the shell-privileged [UserService].
 */
@Singleton
class ShizukuController @Inject constructor(
    @ApplicationContext private val context: Context,
) : PermissionController {

    private val _status = MutableStateFlow(currentStatus())
    override val status: StateFlow<ShizukuStatus> = _status.asStateFlow()

    private var userService: IUserService? = null
    private val bindMutex = Mutex()

    private val userServiceArgs by lazy {
        Shizuku.UserServiceArgs(ComponentName(context.packageName, UserService::class.java.name))
            .daemon(false)
            .processNameSuffix("perm")
            .debuggable(BuildConfig.DEBUG)
            .version(BuildConfig.VERSION_CODE)
    }

    /** Call once from Application.onCreate so the status stays current. */
    fun init() {
        Shizuku.addBinderReceivedListenerSticky { refreshStatus() }
        Shizuku.addBinderDeadListener {
            userService = null // drop the stale binder so the next call re-binds
            refreshStatus()
        }
        Shizuku.addRequestPermissionResultListener { _, _ -> refreshStatus() }
    }

    override fun refreshStatus() {
        _status.value = currentStatus()
    }

    /** Shows Shizuku's own "Allow this app?" dialog. */
    override fun requestPermission() {
        if (!Shizuku.pingBinder()) return
        if (Shizuku.isPreV11()) return // very old Shizuku, which this app doesn't support
        Shizuku.requestPermission(PERMISSION_REQUEST_CODE)
    }

    override suspend fun grant(packageName: String, permission: String): Result<Unit> =
        runPm("grant", packageName, permission)

    override suspend fun revoke(packageName: String, permission: String): Result<Unit> =
        runPm("revoke", packageName, permission)

    private suspend fun runPm(op: String, packageName: String, permission: String): Result<Unit> {
        if (currentStatus() != ShizukuStatus.READY) {
            return Result.failure(IllegalStateException("Shizuku isn't ready. Start and authorise Shizuku, then try again."))
        }
        return try {
            val service = ensureService()
            // Try the `pm` wrapper first, then `cmd package` as a fallback (some ROMs differ).
            val first = service.runCommand(arrayOf("pm", op, packageName, permission))
            if (first == "OK") return Result.success(Unit)
            val second = service.runCommand(arrayOf("cmd", "package", op, packageName, permission))
            if (second == "OK") Result.success(Unit)
            else Result.failure(RuntimeException(first.removePrefix("ERR:")))
        } catch (e: Exception) {
            userService = null // a thrown binder call means the service is gone; re-bind next time
            Result.failure(e)
        }
    }

    private suspend fun ensureService(): IUserService = bindMutex.withLock {
        userService?.let { return it }
        withTimeout(BIND_TIMEOUT_MS) {
            suspendCancellableCoroutine { cont ->
                val conn = object : ServiceConnection {
                    override fun onServiceConnected(name: ComponentName, binder: IBinder?) {
                        val svc = if (binder != null && binder.pingBinder()) {
                            IUserService.Stub.asInterface(binder)
                        } else {
                            null
                        }
                        userService = svc
                        if (svc != null) cont.resume(svc)
                        else cont.resumeWithException(IllegalStateException("Couldn't start the Shizuku helper service."))
                    }

                    override fun onServiceDisconnected(name: ComponentName) {
                        userService = null
                    }
                }
                try {
                    Shizuku.bindUserService(userServiceArgs, conn)
                } catch (e: Exception) {
                    cont.resumeWithException(e)
                }
            }
        }
    }

    private fun currentStatus(): ShizukuStatus = try {
        when {
            !isShizukuInstalled() -> ShizukuStatus.NOT_INSTALLED
            !Shizuku.pingBinder() -> ShizukuStatus.NOT_RUNNING
            Shizuku.isPreV11() -> ShizukuStatus.NEEDS_PERMISSION
            Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED -> ShizukuStatus.NEEDS_PERMISSION
            else -> ShizukuStatus.READY
        }
    } catch (e: Exception) {
        // Binder can die between checks; treat as not running rather than crashing.
        if (isShizukuInstalled()) ShizukuStatus.NOT_RUNNING else ShizukuStatus.NOT_INSTALLED
    }

    private fun isShizukuInstalled(): Boolean = try {
        context.packageManager.getPackageInfo(SHIZUKU_PACKAGE, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 4211
        private const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
        private const val BIND_TIMEOUT_MS = 10_000L
    }
}
