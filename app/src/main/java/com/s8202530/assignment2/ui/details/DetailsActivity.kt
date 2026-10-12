package com.s8202530.assignment2.ui.details

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.IntentCompat
import com.s8202530.assignment2.R
import com.s8202530.assignment2.data.model.Entity
import com.s8202530.assignment2.databinding.ActivityDetailsBinding
import com.s8202530.assignment2.databinding.ItemDetailRowBinding
import com.s8202530.assignment2.ui.common.applySystemBarPadding
import dagger.hilt.android.AndroidEntryPoint

/**
 * Screen 3: Details. Shows every field of the selected entity, plus its
 * description. The entity arrives ready to use from the Dashboard, so no
 * network call is needed here.
 */
@AndroidEntryPoint
class DetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarPadding()

        binding.toolbar.setNavigationOnClickListener { finish() }

        val entity = IntentCompat.getParcelableExtra(intent, EXTRA_ENTITY, Entity::class.java)
        if (entity == null) {
            finish() // opened without an entity: nothing to show
            return
        }
        show(entity)
    }

    private fun show(entity: Entity) {
        val empty = getString(R.string.empty_value)

        binding.headerLabel.text = entity.fields.firstOrNull()?.label.orEmpty()
        binding.headerTitle.text = entity.title.ifBlank { empty }

        // One label/value row per field, in the order the API sent them.
        binding.fieldList.removeAllViews()
        entity.fields.forEach { field ->
            val row = ItemDetailRowBinding.inflate(layoutInflater, binding.fieldList, true)
            row.rowLabel.text = field.label
            row.rowValue.text = field.value.ifBlank { empty }
        }

        binding.descriptionText.text = entity.description ?: getString(R.string.no_description)
    }

    companion object {
        private const val EXTRA_ENTITY = "entity"

        fun newIntent(context: Context, entity: Entity): Intent =
            Intent(context, DetailsActivity::class.java).putExtra(EXTRA_ENTITY, entity)
    }
}
