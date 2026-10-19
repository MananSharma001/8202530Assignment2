package com.s8202530.assignment2.ui.dashboard

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.s8202530.assignment2.R
import com.s8202530.assignment2.databinding.ActivityDashboardBinding
import com.s8202530.assignment2.ui.common.applySystemBarPadding
import com.s8202530.assignment2.ui.details.DetailsActivity
import com.s8202530.assignment2.ui.login.LoginActivity
import com.s8202530.assignment2.ui.permissions.PermissionsActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Screen 2: Dashboard. Shows the entities for the keypass in a RecyclerView.
 * Tapping one opens the Details screen.
 */
@AndroidEntryPoint
class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private val viewModel: DashboardViewModel by viewModels()
    private val adapter = EntityAdapter { entity ->
        startActivity(DetailsActivity.newIntent(this, entity))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarPadding()

        binding.toolbar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_log_out) {
                logOut()
                true
            } else {
                false
            }
        }

        binding.topicText.text = getString(R.string.dashboard_topic, viewModel.keypass.replaceFirstChar { it.uppercase() })
        binding.permissionsCard.setOnClickListener {
            startActivity(PermissionsActivity.newIntent(this))
        }
        binding.entityList.adapter = adapter
        binding.swipeRefresh.setOnRefreshListener { viewModel.load() }
        binding.retryButton.setOnClickListener { viewModel.load() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { render(it) }
            }
        }
    }

    private fun render(state: DashboardViewModel.UiState) {
        val loading = state is DashboardViewModel.UiState.Loading
        val hasItems = adapter.itemCount > 0

        // Big spinner the first time; the pull to refresh spinner after that.
        binding.progress.isVisible = loading && !hasItems
        binding.swipeRefresh.isRefreshing = loading && hasItems

        binding.errorGroup.isVisible = state is DashboardViewModel.UiState.Error
        binding.emptyText.isVisible = false

        when (state) {
            is DashboardViewModel.UiState.Loading -> Unit
            is DashboardViewModel.UiState.Error -> {
                binding.errorText.setText(state.message)
                adapter.submitList(emptyList())
            }
            is DashboardViewModel.UiState.Success -> {
                adapter.submitList(state.data.entities)
                binding.countText.text = resources.getQuantityString(
                    R.plurals.entity_count, state.data.total, state.data.total,
                )
                binding.emptyText.isVisible = state.data.entities.isEmpty()
            }
        }
    }

    /** Back to Login with nothing left behind it. */
    private fun logOut() {
        startActivity(
            Intent(this, LoginActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
    }

    companion object {
        const val EXTRA_KEYPASS = "keypass"

        fun newIntent(context: Context, keypass: String): Intent =
            Intent(context, DashboardActivity::class.java).putExtra(EXTRA_KEYPASS, keypass)
    }
}
