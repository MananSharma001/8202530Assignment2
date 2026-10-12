package com.s8202530.assignment2.ui.dashboard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.s8202530.assignment2.R
import com.s8202530.assignment2.data.model.Entity
import com.s8202530.assignment2.databinding.ItemEntityBinding

/**
 * RecyclerView adapter for the Dashboard. Each card shows a summary of one
 * entity: its first field as the title, then the other fields. The
 * description is left out here and shown on the Details screen.
 */
class EntityAdapter(
    private val onClick: (Entity) -> Unit,
) : ListAdapter<Entity, EntityAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ItemEntityBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    inner class ViewHolder(private val binding: ItemEntityBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(entity: Entity) {
            val context = binding.root.context
            val titleField = entity.fields.firstOrNull()

            binding.entityLabel.text = titleField?.label.orEmpty()
            binding.entityTitle.text = entity.title.ifBlank { context.getString(R.string.empty_value) }

            val summary = entity.summaryFields.joinToString("\n") {
                context.getString(R.string.field_line, it.label, it.value.ifBlank { context.getString(R.string.empty_value) })
            }
            binding.entitySummary.isVisible = summary.isNotEmpty()
            binding.entitySummary.text = summary

            binding.root.setOnClickListener { onClick(entity) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<Entity>() {
        override fun areItemsTheSame(oldItem: Entity, newItem: Entity) = oldItem.fields == newItem.fields
        override fun areContentsTheSame(oldItem: Entity, newItem: Entity) = oldItem == newItem
    }
}
