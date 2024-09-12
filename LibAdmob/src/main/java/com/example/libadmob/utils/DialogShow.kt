package com.example.libadmob.utils

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.Transformation
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.example.libadmob.R
import com.example.libadmob.utils.OptAnimationLoader.animationLoaded
import com.example.libadmob.utils.UtilsView.getViewDrawable
import com.pnikosis.materialishprogress.ProgressWheel


class DialogShow @JvmOverloads constructor(context: Context?, alertType: Int = NORMAL_TYPE) :
    Dialog(
        context!!, if (DARK_STYLE) R.style.alert_dialog_dark else R.style.alert_dialog_light
    ), View.OnClickListener {
    private var mDialogView: View? = null
    private val mModalInAnim: AnimationSet?
    private val mModalOutAnim: AnimationSet?
    private val mOverlayOutAnim: Animation
    private val mErrorInAnim: Animation?
    private val mErrorXInAnim: AnimationSet?
    private val mSuccessLayoutAnimSet: AnimationSet?
    private val mSuccessBowAnim: Animation?
    private var mTitleTextView: TextView? = null
    private var mContentTextView: TextView? = null
    private var mCustomViewContainer: FrameLayout? = null
    private var mCustomView: View? = null
    var titleText: String? = null
        private set
    var contentText: String? = null
        private set
    var isShowCancelButton: Boolean = false
        private set
    var isShowContentText: Boolean = false
        private set
    var cancelText: String? = null
        private set
    var confirmText: String? = null
        private set
    private var mNeutralText: String? = null
    var alertType: Int
        private set
    private var mErrorFrame: FrameLayout? = null
    private var mSuccessFrame: FrameLayout? = null
    private var mProgressFrame: FrameLayout? = null
    private var mSuccessTick: SuccessView? = null
    private var mErrorX: ImageView? = null
    private var mSuccessLeftMask: View? = null
    private var mSuccessRightMask: View? = null
    private var mCustomImgDrawable: Drawable? = null
    private var mCustomImage: ImageView? = null
    private var mButtonsContainer: LinearLayout? = null
    private var mConfirmButton: Button? = null
    private var mHideConfirmButton = false
    private var mCancelButton: Button? = null
    private var mNeutralButton: Button? = null
    var confirmButtonBackgroundColor: Int? = null
        private set
    var confirmButtonTextColor: Int? = null
        private set
    var neutralButtonBackgroundColor: Int? = null
        private set
    var neutralButtonTextColor: Int? = null
        private set
    var cancelButtonBackgroundColor: Int? = null
        private set
    var cancelButtonTextColor: Int? = null
        private set
    val progressHelper: ProgressHelper
    private var mWarningFrame: FrameLayout? = null
    private var mCancelClickListener: OnSweetClickListener? = null
    private var mConfirmClickListener: OnSweetClickListener? = null
    private var mNeutralClickListener: OnSweetClickListener? = null
    private var mCloseFromCancel = false
    var isHideKeyBoardOnDismiss: Boolean = true
        private set
    var contentTextSize: Int = 0
        private set

    private val defStrokeWidth: Float
    private var strokeWidth = 0f

    interface OnSweetClickListener {
        fun onClick(dialogShow: DialogShow?)
    }
    companion object {
        const val NORMAL_TYPE: Int = 0
        const val ERROR_TYPE: Int = 1
        const val SUCCESS_TYPE: Int = 2
        const val WARNING_TYPE: Int = 3
        const val CUSTOM_IMAGE_TYPE: Int = 4
        const val PROGRESS_TYPE: Int = 5


        var DARK_STYLE: Boolean = false

        //aliases
        const val BUTTON_CONFIRM: Int = BUTTON_POSITIVE
        const val BUTTON_CANCEL: Int = BUTTON_NEGATIVE

        fun spToPx(sp: Float, context: Context): Int {
            return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                sp,
                context.resources.displayMetrics
            ).toInt()
        }
    }
    init {
        setCancelable(true)
        setCanceledOnTouchOutside(true) //TODO was false

        defStrokeWidth = getContext().resources.getDimension(R.dimen.buttons_stroke_width)
        strokeWidth = defStrokeWidth

        progressHelper = ProgressHelper(context!!)
        this.alertType = alertType
        mErrorInAnim = animationLoaded(getContext(), R.anim.error_frame_in)
        mErrorXInAnim = animationLoaded(getContext(), R.anim.error_x_in) as AnimationSet?
        // 2.3.x system don't support alpha-animation on layer-list drawable
        // remove it from animation set
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.GINGERBREAD_MR1) {
            val childAnims = mErrorXInAnim!!.animations
            var idx = 0
            while (idx < childAnims.size) {
                if (childAnims[idx] is AlphaAnimation) {
                    break
                }
                idx++
            }
            if (idx < childAnims.size) {
                childAnims.removeAt(idx)
            }
        }
        mSuccessBowAnim = animationLoaded(getContext(), R.anim.success_bow_roate)
        mSuccessLayoutAnimSet =
            animationLoaded(getContext(), R.anim.success_mask_layout) as AnimationSet?
        mModalInAnim = animationLoaded(getContext(), R.anim.modal_in) as AnimationSet?
        mModalOutAnim = animationLoaded(getContext(), R.anim.modal_out) as AnimationSet?
        mModalOutAnim!!.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {
            }

            override fun onAnimationEnd(animation: Animation) {
                mDialogView!!.visibility = View.GONE
                if (isHideKeyBoardOnDismiss) {
                    hideSoftKeyboard()
                }
                mDialogView!!.post {
                    if (mCloseFromCancel) {
                        super@DialogShow.cancel()
                    } else {
                        super@DialogShow.dismiss()
                    }
                }
            }

            override fun onAnimationRepeat(animation: Animation) {
            }
        })
        // dialog overlay fade out
        mOverlayOutAnim = object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
                val wlp = window!!.attributes
                wlp.alpha = 1 - interpolatedTime
                window!!.attributes = wlp
            }
        }
        mOverlayOutAnim.setDuration(120)
    }

    override fun onCreate(savedInstanceState: Bundle) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.alert_dialog)

        mDialogView = window!!.decorView.findViewById(android.R.id.content)
        mTitleTextView = findViewById(R.id.title_text)
        mContentTextView = findViewById(R.id.content_text)
        mCustomViewContainer = findViewById(R.id.custom_view_container)
        mErrorFrame = findViewById(R.id.error_frame)
        mErrorX = mErrorFrame!!.findViewById(R.id.error_x)
        mSuccessFrame = findViewById(R.id.success_frame)
        mProgressFrame = findViewById(R.id.progress_dialog)
        mSuccessTick = mSuccessFrame!!.findViewById(R.id.success_tick)
        mSuccessLeftMask = mSuccessFrame!!.findViewById(R.id.mask_left)
        mSuccessRightMask = mSuccessFrame!!.findViewById(R.id.mask_right)
        mCustomImage = findViewById(R.id.custom_image)
        mWarningFrame = findViewById(R.id.warning_frame)
        mButtonsContainer = findViewById(R.id.buttons_container)
        mConfirmButton = findViewById(R.id.confirm_button)
        mConfirmButton!!.setOnClickListener(this)
        mConfirmButton!!.setOnTouchListener(Constants.FOCUS_TOUCH_LISTENER)
        mCancelButton = findViewById(R.id.cancel_button)
        mCancelButton!!.setOnClickListener(this)
        mCancelButton!!.setOnTouchListener(Constants.FOCUS_TOUCH_LISTENER)
        mNeutralButton = findViewById(R.id.neutral_button)
        mNeutralButton!!.setOnClickListener(this)
        mNeutralButton!!.setOnTouchListener(Constants.FOCUS_TOUCH_LISTENER)
        progressHelper.progressWheel = findViewById<View>(R.id.progressWheel) as ProgressWheel

        setTextTitle(titleText)
        setTextContent(contentText)
        setViewCustom(mCustomView)
        setTextCancel(cancelText)
        setTextComfirm(confirmText)
        setTextNeutral(mNeutralText)
        applyStroke()
        setConfirmButtonBackgroundColor(confirmButtonBackgroundColor)
        setConfirmButtonTextColor(confirmButtonTextColor)
        setCancelButtonBackgroundColor(cancelButtonBackgroundColor)
        setCancelButtonTextColor(cancelButtonTextColor)
        setNeutralButtonBackgroundColor(neutralButtonBackgroundColor)
        setNeutralButtonTextColor(neutralButtonTextColor)
        AlertTypeChange(alertType, true)
    }

    private fun adjustButtonContainerVisibility() {
        var showButtonsContainer = false
        for (i in 0 until mButtonsContainer!!.childCount) {
            val view = mButtonsContainer!!.getChildAt(i)
            if (view is Button && view.getVisibility() == View.VISIBLE) {
                showButtonsContainer = true
                break
            }
        }
        mButtonsContainer!!.visibility = if (showButtonsContainer) View.VISIBLE else View.GONE
    }
    private fun restore() {
        mCustomImage!!.visibility = View.GONE
        mErrorFrame!!.visibility = View.GONE
        mSuccessFrame!!.visibility = View.GONE
        mWarningFrame!!.visibility = View.GONE
        mProgressFrame!!.visibility = View.GONE

        mConfirmButton!!.visibility =
            if (mHideConfirmButton) View.GONE else View.VISIBLE

        adjustButtonContainerVisibility()

        mConfirmButton!!.setBackgroundResource(R.drawable.green_button_background)
        mErrorFrame!!.clearAnimation()
        mErrorX!!.clearAnimation()
        mSuccessTick!!.clearAnimation()
        mSuccessLeftMask!!.clearAnimation()
        mSuccessRightMask!!.clearAnimation()
    }


    private fun AlertTypeChange(alertType: Int, fromCreate: Boolean) {
        this.alertType = alertType
        // call after created views
        if (mDialogView != null) {
            if (!fromCreate) {
                // restore all of views state before switching alert type
                restore()
            }
            mConfirmButton!!.visibility = if (mHideConfirmButton) View.GONE else View.VISIBLE
            when (this.alertType) {
                ERROR_TYPE -> mErrorFrame!!.visibility = View.VISIBLE
                SUCCESS_TYPE -> {
                    mSuccessFrame!!.visibility = View.VISIBLE
                    // initial rotate layout of success mask
                    mSuccessLeftMask!!.startAnimation(mSuccessLayoutAnimSet!!.animations[0])
                    mSuccessRightMask!!.startAnimation(mSuccessLayoutAnimSet.animations[1])
                }

                WARNING_TYPE -> //                    mConfirmButton.setBackgroundResource(R.drawable.red_button_background);
                    mWarningFrame!!.visibility = View.VISIBLE

                CUSTOM_IMAGE_TYPE -> setImageCustom(mCustomImgDrawable)
                PROGRESS_TYPE -> {
                    mProgressFrame!!.visibility = View.VISIBLE
                    mConfirmButton!!.visibility = View.GONE
                }
            }
            adjustButtonContainerVisibility()
            if (!fromCreate) {
                animationPlayed()
            }
        }
    }

    private fun animationPlayed() {
        if (alertType == ERROR_TYPE) {
            mErrorFrame!!.startAnimation(mErrorInAnim)
            mErrorX!!.startAnimation(mErrorXInAnim)
        } else if (alertType == SUCCESS_TYPE) {
            mSuccessTick!!.startAnimTick()
            mSuccessRightMask!!.startAnimation(mSuccessBowAnim)
        }
    }

    fun setTextTitle(text: String?): DialogShow {
        titleText = text
        if (mTitleTextView != null && titleText != null) {
            if (text!!.isEmpty()) {
                mTitleTextView!!.visibility = View.GONE
            } else {
                mTitleTextView!!.visibility = View.VISIBLE
                mTitleTextView!!.text = Html.fromHtml(titleText)
            }
        }
        return this
    }



    fun setTextContent(text: String?): DialogShow {
        contentText = text
        if (mContentTextView != null && contentText != null) {
            showContentText(true)
            if (contentTextSize != 0) {
                mContentTextView!!.setTextSize(
                    TypedValue.COMPLEX_UNIT_PX,
                    spToPx(contentTextSize.toFloat(), context).toFloat()
                )
            }
            mContentTextView!!.text = Html.fromHtml(contentText)
            mContentTextView!!.visibility = View.VISIBLE
            mCustomViewContainer!!.visibility = View.GONE
        }
        return this
    }
    fun setImageCustom(drawable: Drawable?): DialogShow {
        mCustomImgDrawable = drawable
        if (mCustomImage != null && mCustomImgDrawable != null) {
            mCustomImage!!.visibility = View.VISIBLE
            mCustomImage!!.setImageDrawable(mCustomImgDrawable)
        }
        return this
    }
    private fun applyStroke() {
        if (java.lang.Float.compare(defStrokeWidth, strokeWidth) != 0) {
            val r = context.resources
            setButtonBackgroundColor(mConfirmButton, r.getColor(R.color.main_green_color))
            setButtonBackgroundColor(mNeutralButton, r.getColor(R.color.main_disabled_color))
            setButtonBackgroundColor(mCancelButton, r.getColor(R.color.red_btn_bg_color))
        }
    }



    fun showContentText(isShow: Boolean): DialogShow {
        isShowContentText = isShow
        if (mContentTextView != null) {
            mContentTextView!!.visibility =
                if (isShowContentText) View.VISIBLE else View.GONE
        }
        return this
    }

    fun setTextCancel(text: String?): DialogShow {
        cancelText = text
        if (mCancelButton != null && cancelText != null) {
            showCancelButton(true)
            mCancelButton!!.text = cancelText
        }
        return this
    }

    fun setTextComfirm(text: String?): DialogShow {
        confirmText = text
        if (mConfirmButton != null && confirmText != null) {
            mConfirmButton!!.text = confirmText
        }
        return this
    }
    fun showCancelButton(isShow: Boolean): DialogShow {
        isShowCancelButton = isShow
        if (mCancelButton != null) {
            mCancelButton!!.visibility =
                if (isShowCancelButton) View.VISIBLE else View.GONE
        }
        return this
    }
    fun setConfirmButtonBackgroundColor(color: Int?): DialogShow {
        confirmButtonBackgroundColor = color
        setButtonBackgroundColor(mConfirmButton, color)
        return this
    }

    fun setNeutralButtonBackgroundColor(color: Int?): DialogShow {
        neutralButtonBackgroundColor = color
        setButtonBackgroundColor(mNeutralButton, color)
        return this
    }



    private fun setButtonBackgroundColor(btn: Button?, color: Int?) {
        if (btn != null && color != null) {
            val drawableItems = getViewDrawable(btn)
            if (drawableItems != null) {
                val gradientDrawableUnChecked = drawableItems[1] as GradientDrawable
                //solid color
                gradientDrawableUnChecked.setColor(color)
                //stroke
                gradientDrawableUnChecked.setStroke(strokeWidth.toInt(), genStrokeColor(color))
            }
        }
    }
    fun setCancelButtonBackgroundColor(color: Int?): DialogShow {
        cancelButtonBackgroundColor = color
        setButtonBackgroundColor(mCancelButton, color)
        return this
    }
    private fun genStrokeColor(color: Int): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[2] *= 0.7f // decrease value component
        return Color.HSVToColor(hsv)
    }



    fun setNeutralButtonTextColor(color: Int?): DialogShow {
        neutralButtonTextColor = color
        if (mNeutralButton != null && color != null) {
            mNeutralButton!!.setTextColor(neutralButtonTextColor!!)
        }
        return this
    }
    fun setConfirmButtonTextColor(color: Int?): DialogShow {
        confirmButtonTextColor = color
        if (mConfirmButton != null && color != null) {
            mConfirmButton!!.setTextColor(confirmButtonTextColor!!)
        }
        return this
    }
    fun setCancelButtonTextColor(color: Int?): DialogShow {
        cancelButtonTextColor = color
        if (mCancelButton != null && color != null) {
            mCancelButton!!.setTextColor(cancelButtonTextColor!!)
        }
        return this
    }





    override fun setTitle(title: CharSequence?) {
        this.setTextTitle(title.toString())
    }

    override fun setTitle(titleId: Int) {
        this.setTextTitle(context.resources.getString(titleId))
    }
    fun setTextNeutral(text: String?): DialogShow {
        mNeutralText = text
        if (mNeutralButton != null && mNeutralText != null && !text!!.isEmpty()) {
            mNeutralButton!!.visibility = View.VISIBLE
            mNeutralButton!!.text = mNeutralText
        }
        return this
    }
    override fun onStart() {
        mDialogView!!.startAnimation(mModalInAnim)
        animationPlayed()
    }

    fun setViewCustom(view: View?): DialogShow {
        mCustomView = view
        if (mCustomView != null && mCustomViewContainer != null) {
            mCustomViewContainer!!.addView(view)
            mCustomViewContainer!!.visibility = View.VISIBLE
            mContentTextView!!.visibility = View.GONE
        }
        return this
    }

    override fun cancel() {
        dismissAnimation(true)
    }


    fun dismissAnimation() {
        dismissAnimation(false)
    }



    override fun onClick(v: View) {
        if (v.id == R.id.cancel_button) {
            if (mCancelClickListener != null) {
                mCancelClickListener!!.onClick(this@DialogShow)
            } else {
                dismissAnimation()
            }
        } else if (v.id == R.id.confirm_button) {
            if (mConfirmClickListener != null) {
                mConfirmClickListener!!.onClick(this@DialogShow)
            } else {
                dismissAnimation()
            }
        } else if (v.id == R.id.neutral_button) {
            if (mNeutralClickListener != null) {
                mNeutralClickListener!!.onClick(this@DialogShow)
            } else {
                dismissAnimation()
            }
        }
    }
    private fun dismissAnimation(fromCancel: Boolean) {
        mCloseFromCancel = fromCancel
        //several view animations can't be launched at one view, that's why apply alpha animation on child
        (mDialogView as ViewGroup?)!!.getChildAt(0)
            .startAnimation(mOverlayOutAnim) //alpha animation
        mDialogView!!.startAnimation(mModalOutAnim) //scale animation
    }
    private fun hideSoftKeyboard() {
        val activity = ownerActivity
        if (activity != null) {
            val inputMethodManager =
                activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
            if (inputMethodManager != null && activity.currentFocus != null) {
                inputMethodManager.hideSoftInputFromWindow(activity.currentFocus!!.windowToken, 0)
            }
        }
    }


}