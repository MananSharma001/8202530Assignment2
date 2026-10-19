package com.s8202530.assignment2.ui.permissions

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.s8202530.assignment2.R
import com.s8202530.assignment2.data.model.AppSummary
import com.s8202530.assignment2.data.model.PermissionGroup
import com.s8202530.assignment2.databinding.ItemAppBinding
import com.s8202530.assignment2.ui.common.AppIconLoader
import kotlinx.coroutines.CoroutineScope

/** One row per installed app: icon, name, how many permissions are allowed. */
class AppsAdapter(
    private val iconLoader: AppIconLoader,
    private val scope: CoroutineScope,
    private val onClick: (AppSummary) -> Unit,
) : ListAdapter<AppSummary, AppsAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ItemAppBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    inner class ViewHolder(private val binding: ItemAppBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(app: AppSummary) {
            val context = binding.root.context

            binding.appName.text = app.label
            binding.appSummary.text = context.getString(
                if (app.isSystem) R.string.app_allowed_count_system else R.string.app_allowed_count,
                app.grantedCount,
                app.requestedCount,
            )

            // Highlight the three most sensitive permissions.
            val sensitive = SENSITIVE_GROUPS
                .filter { it in app.grantedGroups }
                .joinToString(", ") { context.getString(it.label) }
            binding.appSensitive.isVisible = sensitive.isNotEmpty()
            binding.appSensitive.text = context.getString(R.string.app_uses, sensitive)

            iconLoader.load(app.packageName, binding.appIcon, scope)
            binding.root.setOnClickListener { onClick(app) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<AppSummary>() {
        override fun areItemsTheSame(oldItem: AppSummary, newItem: AppSummary) =
            oldItem.packageName == newItem.packageName

        override fun areContentsTheSame(oldItem: AppSummary, newItem: AppSummary) =
            oldItem == newItem
    }

    private companion object {
        val SENSITIVE_GROUPS = listOf(
            PermissionGroup.LOCATION,
            PermissionGroup.CAMERA,
            PermissionGroup.MICROPHONE,
        )
    }
}
