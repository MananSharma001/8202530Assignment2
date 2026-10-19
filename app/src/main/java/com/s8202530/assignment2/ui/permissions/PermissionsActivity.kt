package com.s8202530.assignment2.ui.permissions

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.s8202530.assignment2.R
import com.s8202530.assignment2.databinding.ActivityPermissionsBinding
import com.s8202530.assignment2.databinding.ItemStatBinding
import com.s8202530.assignment2.ui.common.AppIconLoader
import com.s8202530.assignment2.ui.common.applySystemBarPadding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Permission Manager: a summary of what your apps are allowed to do, then
 * every app in a searchable list. Tapping one opens [AppPermissionsActivity].
 */
@AndroidEntryPoint
class PermissionsActivity : AppCompatActivity() {

    @Inject lateinit var iconLoader: AppIconLoader

    private lateinit var binding: ActivityPermissionsBinding
    private val viewModel: PermissionsViewModel by viewModels()
    private lateinit var adapter: AppsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityPermissionsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarPadding()

        binding.toolbar.setNavigationOnClickListener { finish() }

        setUpStat(binding.statLocation, R.drawable.ic_location, R.string.stat_location)
        setUpStat(binding.statCamera, R.drawable.ic_camera, R.string.stat_camera)
        setUpStat(binding.statMicrophone, R.drawable.ic_mic, R.string.stat_microphone)

        adapter = AppsAdapter(iconLoader, lifecycleScope) { app ->
            startActivity(AppPermissionsActivity.newIntent(this, app.packageName))
        }
        binding.appList.adapter = adapter

        binding.searchInput.doAfterTextChanged { text ->
            viewModel.setQuery(text?.toString().orEmpty())
        }
        binding.filterChips.setOnCheckedStateChangeListener { _, checkedIds ->
            val filter = when (checkedIds.firstOrNull()) {
                R.id.chip_location -> AppFilter.LOCATION
                R.id.chip_camera -> AppFilter.CAMERA
                R.id.chip_microphone -> AppFilter.MICROPHONE
                else -> AppFilter.ALL
            }
            viewModel.setFilter(filter)
        }
        binding.systemSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setShowSystem(isChecked)
        }
        binding.swipeRefresh.setOnRefreshListener { viewModel.refresh(userInitiated = true) }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { render(it) }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Rescan every time the screen shows, so changes made in Settings appear.
        viewModel.refresh()
    }

    private fun render(state: PermissionsViewModel.UiState) {
        val apps = state.visibleApps
        adapter.submitList(apps)

        binding.progress.isVisible = state.loading
        binding.swipeRefresh.isRefreshing = state.refreshing
        binding.countText.text = resources.getQuantityString(R.plurals.apps_count, apps.size, apps.size)
        binding.emptyText.isVisible = !state.loading && apps.isEmpty()

        val stats = state.stats
        binding.grantedValue.text = getString(R.string.stat_granted_value, stats.grantedTotal, stats.requestedTotal)
        binding.grantedProgress.setProgressCompat(stats.grantedPercent, true)
        binding.grantedCaption.text = getString(R.string.stat_granted_caption, stats.grantedPercent, stats.appCount)
        binding.statLocation.statValue.text = stats.locationApps.toString()
        binding.statCamera.statValue.text = stats.cameraApps.toString()
        binding.statMicrophone.statValue.text = stats.microphoneApps.toString()
    }

    private fun setUpStat(stat: ItemStatBinding, @DrawableRes icon: Int, @StringRes label: Int) {
        stat.statIcon.setImageResource(icon)
        stat.statLabel.setText(label)
    }

    companion object {
        fun newIntent(context: Context): Intent = Intent(context, PermissionsActivity::class.java)
    }
}
