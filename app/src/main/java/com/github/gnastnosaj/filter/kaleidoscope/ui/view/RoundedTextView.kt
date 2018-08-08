package com.github.gnastnosaj.filter.kaleidoscope.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.ViewManager
import android.widget.TextView
import org.jetbrains.anko.custom.ankoView

class RoundedTextView(context: Context) : TextView(context) {
    private val paint = Paint()

    init {
        paint.isAntiAlias = true
    }

    override fun setBackgroundColor(color: Int) {
        paint.color = color
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(Math.max(measuredWidth, measuredHeight), Math.max(measuredWidth, measuredHeight))
    }

    override fun draw(canvas: Canvas) {
        canvas.drawCircle(width.toFloat() / 2, height.toFloat() / 2, Math.max(width, height).toFloat() / 2, paint)
        super.draw(canvas)
    }
}

inline fun ViewManager.roundedTextView() = roundedTextView {}

inline fun ViewManager.roundedTextView(init: RoundedTextView.() -> Unit): RoundedTextView {
    return ankoView({ RoundedTextView(it) }, theme = 0, init = init)
}