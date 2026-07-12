package com.villageclinicledger.ui.util

import android.content.Context
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

/**
 * Utility to scale UI layouts proportionally from the Samsung A51 portrait reference frame (1080 x 2400 px)
 * to any other Android device screen resolution at runtime.
 */
object LayoutScaler {

    fun getScaledX(context: Context, designX: Int): Int {
        val screenWidth = context.resources.displayMetrics.widthPixels
        val scaleX = screenWidth.toFloat() / 1080f
        return (designX * scaleX).toInt()
    }

    fun getScaledY(context: Context, designY: Int): Int {
        val screenHeight = context.resources.displayMetrics.heightPixels
        val scaleY = screenHeight.toFloat() / 2400f
        return (designY * scaleY).toInt()
    }

    fun getScaledSize(context: Context, designSize: Int, isHorizontal: Boolean): Int {
        return if (isHorizontal) {
            getScaledX(context, designSize)
        } else {
            getScaledY(context, designSize)
        }
    }

    /**
     * Programmatically positions and sizes a view using absolute coordinates from A51 reference.
     * Modifies the view's layout parameters (width, height, and margins).
     */
    fun scaleAndPositionView(
        view: View,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        paddingLeft: Int = 0,
        paddingTop: Int = 0,
        paddingRight: Int = 0,
        paddingBottom: Int = 0
    ) {
        val context = view.context
        val scaledX = getScaledX(context, x)
        val scaledY = getScaledY(context, y)
        val scaledW = if (w > 0) getScaledX(context, w) else w
        val scaledH = if (h > 0) getScaledY(context, h) else h

        val lp = view.layoutParams
        if (lp is ViewGroup.MarginLayoutParams) {
            lp.width = scaledW
            lp.height = scaledH
            lp.leftMargin = scaledX
            lp.topMargin = scaledY
            view.layoutParams = lp
        }

        // Apply scaled padding
        val sPaddingLeft = getScaledX(context, paddingLeft)
        val sPaddingTop = getScaledY(context, paddingTop)
        val sPaddingRight = getScaledX(context, paddingRight)
        val sPaddingBottom = getScaledY(context, paddingBottom)
        view.setPadding(sPaddingLeft, sPaddingTop, sPaddingRight, sPaddingBottom)
    }

    /**
     * Helper to scale text size based on scaleFactor.
     * We scale text size based on scaleX to ensure readability across devices.
     */
    fun scaleTextSize(textView: TextView, designSp: Float) {
        val context = textView.context
        val screenWidth = context.resources.displayMetrics.widthPixels
        val scaleX = screenWidth.toFloat() / 1080f
        val scaledSp = designSp * scaleX
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, scaledSp)
    }
}
