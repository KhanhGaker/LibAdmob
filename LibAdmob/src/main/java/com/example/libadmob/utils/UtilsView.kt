package com.example.libadmob.utils

import android.graphics.drawable.Drawable
import android.graphics.drawable.DrawableContainer
import android.graphics.drawable.StateListDrawable
import android.view.View

object UtilsView {
    @JvmStatic
    fun getViewDrawable(view: View): Array<Drawable>? {
        val drawable = view.background as StateListDrawable
        val dcs = drawable.constantState as DrawableContainer.DrawableContainerState?
        if (dcs != null) {
            return dcs.children
        }
        return null
    }
}