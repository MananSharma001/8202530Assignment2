package com.s8202530.assignment2.ui.permissions

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.snackbar.Snackbar
import com.s8202530.assignment2.R
import com.s8202530.assignment2.data.model.AppDetail
import com.s8202530.assignment2.data.shizuku.ShizukuStatus
import com.s8202530.assignment2.databinding.ActivityAppPermissionsBinding
import com.s8202530.assignment2.ui.common.AppIconLoader
import com.s8202530.assignment2.ui.common.applySystemBarPadding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Every runtime permission one app asked for, each with a switch that
 * allows or denies it right here (through Shizuku, no root needed).
 * Reads again every 2 seconds, so a change made anywhere shows up live.
 */
@AndroidEntryPoint
class AppPermissionsActivity : AppCompatActivity() {

    @Inject lateinit var iconLoader: AppIconLoader

    private lateinit var binding: ActivityAppPermissionsBinding
    private val viewModel: AppPermissionsViewModel by viewModels()
    private val permissionAdapter by lazy { PermissionAdapter(viewModel::toggle) }
    private var pulse: ObjectAnimator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityAppPermissionsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarPadding()

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.permissionList.adapter = permissionAdapter

        pulse = ObjectAnimator.ofFloat(binding.liveDot, View.ALPHA, 1f, 0.25f).apply {
            duration = 900
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            start()
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.state.collect { render(it) } }
                launch { viewModel.shizukuStatus.collect { showShizukuBanner(it) } }
                launch {
                    viewModel.toggleFailed.collect { failed ->
                        Snackbar.make(
                            binding.root,
                            getString(R.string.toggle_failed, failed.permissionLabel, failed.reason),
                            Snackbar.LENGTH_LONG,
                        ).show()
                    }
                }
                launch {
                    viewModel.needsShizuku.collect { status ->
                        val step = shizukuStep(status) ?: return@collect
                        Snackbar.make(binding.root, step.message, Snackbar.LENGTH_LONG)
                            .setAction(step.action) { step.run() }
                            .show()
                    }
                }
            }
        }

        // Live mode: read the permissions again every 2 seconds while visible.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                while (true) {
                    viewModel.sync()
                    delay(LIVE_REFRESH_MS)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshShizuku() // Shizuku may have been started while we were away
    }

    private fun render(state: AppPermissionsViewModel.UiState) {
        val detail = state.detail
        binding.progress.isVisible = state.loading
        binding.notFound.isVisible = state.notFound
        binding.content.isVisible = detail != null
        if (detail != null) show(detail, state.busy)
    }

    private fun show(detail: AppDetail, busy: Set<String>) {
        binding.toolbar.title = detail.label

        iconLoader.load(detail.packageName, binding.appIcon, lifecycleScope)
        binding.appName.text = detail.label
        binding.packageName.text = detail.packageName
        binding.appMeta.text = getString(
            if (detail.isSystem) R.string.detail_meta_system else R.string.detail_meta,
            detail.versionName ?: getString(R.string.unknown_version),
            detail.targetSdk,
        )
        binding.legacyNote.isVisible = detail.isLegacy

        binding.permissionsTitle.text = getString(
            R.string.permissions_header,
            detail.grantedCount,
            detail.permissions.size,
        )
        // Apps built for Android 5.1 or older can't have permissions taken away one by one.
        permissionAdapter.submitList(
            detail.permissions.map { PermissionAdapter.Row(it, busy = it.name in busy, locked = detail.isLegacy) }
        )
        binding.noPermissions.isVisible = detail.permissions.isEmpty()
    }

    /** The next thing to do before the switches work, or null once Shizuku is ready. */
    private class ShizukuStep(val message: String, val action: String, val run: () -> Unit)

    private fun shizukuStep(status: ShizukuStatus): ShizukuStep? = when (status) {
        ShizukuStatus.NOT_INSTALLED -> ShizukuStep(
            getString(R.string.shizuku_not_installed), getString(R.string.shizuku_install), ::openShizukuDownload,
        )
        ShizukuStatus.NOT_RUNNING -> ShizukuStep(
            getString(R.string.shizuku_not_running), getString(R.string.shizuku_open), ::openShizukuApp,
        )
        ShizukuStatus.NEEDS_PERMISSION -> ShizukuStep(
            getString(R.string.shizuku_needs_permission), getString(R.string.shizuku_authorize),
            viewModel::requestShizukuPermission,
        )
        ShizukuStatus.READY -> null
    }

    private fun showShizukuBanner(status: ShizukuStatus) {
        val step = shizukuStep(status)
        binding.shizukuBanner.isVisible = step != null
        if (step == null) return
        binding.shizukuMessage.text = step.message
        binding.shizukuAction.text = step.action
        binding.shizukuAction.setOnClickListener { step.run() }
    }

    private fun openShizukuApp() {
        val intent = packageManager.getLaunchIntentForPackage(SHIZUKU_PACKAGE)
        if (intent != null) startActivity(intent) else openShizukuDownload()
    }

    private fun openShizukuDownload() {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(SHIZUKU_DOWNLOAD_URL)))
    }

    override fun onDestroy() {
        pulse?.cancel()
        pulse = null
        super.onDestroy()
    }

    companion object {
        const val EXTRA_PACKAGE = "packageName"
        private const val LIVE_REFRESH_MS = 2_000L
        private const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
        private const val SHIZUKU_DOWNLOAD_URL = "https://shizuku.rikka.app/download/"

        fun newIntent(context: Context, packageName: String): Intent =
            Intent(context, AppPermissionsActivity::class.java).putExtra(EXTRA_PACKAGE, packageName)
    }
}
