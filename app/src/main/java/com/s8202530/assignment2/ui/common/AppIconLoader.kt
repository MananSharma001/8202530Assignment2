package com.s8202530.assignment2.ui.common

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.util.LruCache
import android.widget.ImageView
import com.s8202530.assignment2.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** Loads app icons off the main thread and keeps recent ones in memory. */
@Singleton
class AppIconLoader @Inject constructor(
    @ApplicationContext context: Context,
    @IoDispatcher private val io: CoroutineDispatcher,
) {

    private val pm: PackageManager = context.packageManager
    private val cache = LruCache<String, Drawable>(150)

    fun load(packageName: String, target: ImageView, scope: CoroutineScope) {
        target.tag = packageName

        val cached = cache.get(packageName)
        if (cached != null) {
            target.setImageDrawable(cached.copy())
            return
        }

        target.setImageDrawable(null)
        scope.launch {
            val icon = withContext(io) {
                try {
                    pm.getApplicationIcon(packageName)
                } catch (e: PackageManager.NameNotFoundException) {
                    null
                }
            }
            if (icon != null) cache.put(packageName, icon)
            // The row may have been recycled for another app while we loaded.
            if (target.tag == packageName) {
                target.setImageDrawable(icon?.copy() ?: pm.defaultActivityIcon)
            }
        }
    }

    /** Each ImageView gets its own copy, so two screens can show the same icon. */
    private fun Drawable.copy(): Drawable = constantState?.newDrawable() ?: this
}
