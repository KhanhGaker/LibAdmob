package com.example.libadmob.utils

import android.content.Context
import com.example.libadmob.R
import com.pnikosis.materialishprogress.ProgressWheel

class ProgressHelper(ctx: Context) {
    private var mProgressWheel: ProgressWheel? = null
    var isSpinning: Boolean = true
        private set
    private var mSpinSpeed = 0.75f
    private var mBarWidth: Int
    private var mBarColor: Int
    private var mRimWidth = 0
    private var mRimColor = 0x00000000
    private var mIsInstantProgress = false
    private var mProgressVal: Float
    private var mCircleRadius: Int

    init {
        mBarWidth = ctx.resources.getDimensionPixelSize(R.dimen.common_circle_width) + 1
        mBarColor = ctx.resources.getColor(R.color.success_stroke_color)
        mProgressVal = -1f
        mCircleRadius = ctx.resources.getDimensionPixelOffset(R.dimen.progress_circle_radius)
    }

    var progressWheel: ProgressWheel?
        get() = mProgressWheel
        set(progressWheel) {
            mProgressWheel = progressWheel
            propsIfNeedUpdate()
        }

    private fun propsIfNeedUpdate() {
        if (mProgressWheel != null) {
            if (!isSpinning && mProgressWheel!!.isSpinning) {
                mProgressWheel!!.stopSpinning()
            } else if (isSpinning && !mProgressWheel!!.isSpinning) {
                mProgressWheel!!.spin()
            }
            if (mSpinSpeed != mProgressWheel!!.spinSpeed) {
                mProgressWheel!!.spinSpeed = mSpinSpeed
            }
            if (mBarWidth != mProgressWheel!!.barWidth) {
                mProgressWheel!!.barWidth = mBarWidth
            }
            if (mBarColor != mProgressWheel!!.barColor) {
                mProgressWheel!!.barColor = mBarColor
            }
            if (mRimWidth != mProgressWheel!!.rimWidth) {
                mProgressWheel!!.rimWidth = mRimWidth
            }
            if (mRimColor != mProgressWheel!!.rimColor) {
                mProgressWheel!!.rimColor = mRimColor
            }
            if (mProgressVal != mProgressWheel!!.progress) {
                if (mIsInstantProgress) {
                    mProgressWheel!!.setInstantProgress(mProgressVal)
                } else {
                    mProgressWheel!!.progress = mProgressVal
                }
            }
            if (mCircleRadius != mProgressWheel!!.circleRadius) {
                mProgressWheel!!.circleRadius = mCircleRadius
            }
        }
    }


}
