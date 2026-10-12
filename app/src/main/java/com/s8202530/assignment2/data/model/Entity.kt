package com.s8202530.assignment2.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/** One field of an entity, e.g. key "artistName", label "Artist Name", value "Monet". */
@Parcelize
data class EntityField(
    val key: String,
    val label: String,
    val value: String,
) : Parcelable

/**
 * One item from the dashboard. Parcelable so it can be handed from the
 * Dashboard to the Details screen inside an Intent.
 */
@Parcelize
data class Entity(
    val fields: List<EntityField>, // every property except the description, in API order
    val description: String?,
) : Parcelable {

    /** Main line in the list: the first field's value. */
    val title: String get() = fields.firstOrNull()?.value.orEmpty()

    /** The rest of the summary shown on the Dashboard card (description excluded, as the brief asks). */
    val summaryFields: List<EntityField> get() = fields.drop(1)
}

/** Everything the Dashboard shows. */
data class DashboardData(
    val entities: List<Entity>,
    val total: Int,
)
