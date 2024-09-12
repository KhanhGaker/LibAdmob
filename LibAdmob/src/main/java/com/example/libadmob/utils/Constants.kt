package com.example.libadmob.utils

import android.annotation.SuppressLint
import android.graphics.PorterDuff
import android.view.MotionEvent
import android.view.View.OnTouchListener

object Constants {
    @SuppressLint("ClickableViewAccessibility")
    @JvmField
    val FOCUS_TOUCH_LISTENER: OnTouchListener = OnTouchListener { v, event ->
        val drawable = v.background
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_BUTTON_PRESS -> {
                drawable.setColorFilter(0x20000000, PorterDuff.Mode.SRC_ATOP)
                v.invalidate()
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                drawable.clearColorFilter()
                v.invalidate()
            }
        }
        false
    }
}
