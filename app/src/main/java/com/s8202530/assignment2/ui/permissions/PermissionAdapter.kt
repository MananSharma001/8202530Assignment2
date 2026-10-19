package com.s8202530.assignment2.ui.permissions

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.s8202530.assignment2.R
import com.s8202530.assignment2.data.model.AppPermission
import com.s8202530.assignment2.databinding.ItemPermissionBinding

/** Permission rows: group icon, name, and a switch that allows or denies it. */
class PermissionAdapter(
    private val onToggle: (AppPermission) -> Unit,
) : ListAdapter<PermissionAdapter.Row, PermissionAdapter.ViewHolder>(DiffCallback) {

    /**
     * [busy] while the change is being made: the switch shows where it's going
     * and can't be tapped again. [locked] when Android won't let it change.
     */
    data class Row(val permission: AppPermission, val busy: Boolean, val locked: Boolean)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ItemPermissionBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    inner class ViewHolder(private val binding: ItemPermissionBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(row: Row) {
            val permission = row.permission
            val context = binding.root.context

            binding.groupIcon.setImageResource(permission.group.icon)
            binding.permissionLabel.text = permission.label
            binding.permissionMeta.text = context.getString(
                R.string.permission_meta,
                context.getString(permission.group.label),
                permission.name.removePrefix("android.permission."),
            )

            binding.permissionSwitch.contentDescription = permission.label
            binding.permissionSwitch.isChecked = if (row.busy) !permission.granted else permission.granted
            binding.permissionSwitch.isEnabled = !row.busy && !row.locked
            binding.root.isEnabled = !row.busy && !row.locked

            // The state always comes from the ViewModel, so undo the tap's own flip.
            binding.permissionSwitch.setOnClickListener {
                binding.permissionSwitch.isChecked = permission.granted
                onToggle(permission)
            }
            binding.root.setOnClickListener { onToggle(permission) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<Row>() {
        override fun areItemsTheSame(oldItem: Row, newItem: Row) =
            oldItem.permission.name == newItem.permission.name

        override fun areContentsTheSame(oldItem: Row, newItem: Row) = oldItem == newItem
    }
}
