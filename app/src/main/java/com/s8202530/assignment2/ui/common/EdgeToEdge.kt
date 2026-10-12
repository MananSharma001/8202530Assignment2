package com.s8202530.assignment2.ui.common

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

/**
 * Apps targeting Android 15 draw behind the status bar and navigation bar
 * (edge to edge). This pads the screen's root view so nothing hides under
 * those bars, or under the keyboard when it's open.
 */
fun View.applySystemBarPadding() {
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val bars = insets.getInsets(
            WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
        )
        val keyboard = insets.getInsets(WindowInsetsCompat.Type.ime())
        view.updatePadding(
            left = bars.left,
            top = bars.top,
            right = bars.right,
            bottom = maxOf(bars.bottom, keyboard.bottom),
        )
        WindowInsetsCompat.CONSUMED
    }
}
