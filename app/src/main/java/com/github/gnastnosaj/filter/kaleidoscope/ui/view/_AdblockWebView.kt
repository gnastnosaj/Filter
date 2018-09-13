package com.github.gnastnosaj.filter.kaleidoscope.ui.view

import android.content.Context
import android.support.v4.view.MotionEventCompat
import android.support.v4.view.NestedScrollingChild
import android.support.v4.view.NestedScrollingChildHelper
import android.support.v4.view.ViewCompat
import android.view.MotionEvent
import android.view.ViewManager
import com.github.gnastnosaj.boilerplate.Boilerplate
import com.taobao.android.dexposed.DexposedBridge
import com.taobao.android.dexposed.XC_MethodHook
import com.taobao.android.dexposed.utility.Logger
import org.adblockplus.libadblockplus.android.webview.AdblockWebView
import org.jetbrains.anko.custom.ankoView
import java.lang.reflect.Field
import java.lang.reflect.Modifier


class NestedScrollAdblockWebView(context: Context) : AdblockWebView(context), NestedScrollingChild {
    private var mLastMotionY = 0

    private var mScrollOffset = IntArray(2)
    private var mScrollConsumed = IntArray(2)

    private var mNestedYOffset = 0

    private var mChildHelper: NestedScrollingChildHelper = NestedScrollingChildHelper(this)

    var injectJS: ((injectJS: String) -> Unit)? = null

    companion object {
        @JvmField
        val hook = object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val webView = param.thisObject as? NestedScrollAdblockWebView
                webView?.injectJS?.let {
                    it.invoke(param.args[0] as String)
                    param.result = null
                }
            }
        }

        init {
            val debugField = Logger::class.java.getDeclaredField("DEBUG")
            val modifiersField = Field::class.java.getDeclaredField("accessFlags")
            modifiersField.isAccessible = true
            modifiersField.setInt(debugField, debugField.modifiers and Modifier.FINAL.inv())
            debugField.isAccessible = true
            debugField.set(null, Boilerplate.DEBUG)
            DexposedBridge.findAndHookMethod(AdblockWebView::class.java, "runScript", String::class.java, hook)
        }
    }


    init {
        isNestedScrollingEnabled = true
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        var result = false

        val trackedEvent = MotionEvent.obtain(event)

        val action = MotionEventCompat.getActionMasked(event)

        if (action == MotionEvent.ACTION_DOWN) {
            mNestedYOffset = 0
        }

        val y = event.y.toInt()

        event.offsetLocation(0f, mNestedYOffset.toFloat())

        when (action) {
            MotionEvent.ACTION_DOWN -> {
                mLastMotionY = y
                startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL)
                result = super.onTouchEvent(event)
            }
            MotionEvent.ACTION_MOVE -> {
                var deltaY = mLastMotionY - y

                if (dispatchNestedPreScroll(0, deltaY, mScrollConsumed, mScrollOffset)) {
                    deltaY -= mScrollConsumed[1]
                    trackedEvent.offsetLocation(0f, mScrollOffset[1].toFloat())
                    mNestedYOffset += mScrollOffset[1]
                }

                mLastMotionY = y - mScrollOffset[1]

                val oldY = scrollY
                val newScrollY = Math.max(0, oldY + deltaY)
                val dyConsumed = newScrollY - oldY
                val dyUnconsumed = deltaY - dyConsumed

                if (dispatchNestedScroll(0, dyConsumed, 0, dyUnconsumed, mScrollOffset)) {
                    mLastMotionY -= mScrollOffset[1]
                    trackedEvent.offsetLocation(0f, mScrollOffset[1].toFloat())
                    mNestedYOffset += mScrollOffset[1]
                }

                result = super.onTouchEvent(trackedEvent)
                trackedEvent.recycle()
            }
            MotionEvent.ACTION_POINTER_DOWN, MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                stopNestedScroll()
                result = super.onTouchEvent(event)
            }
        }
        return result
    }

    // NestedScrollingChild

    override fun setNestedScrollingEnabled(enabled: Boolean) {
        mChildHelper.isNestedScrollingEnabled = enabled
    }

    override fun isNestedScrollingEnabled(): Boolean {
        return mChildHelper.isNestedScrollingEnabled
    }

    override fun startNestedScroll(axes: Int): Boolean {
        return mChildHelper.startNestedScroll(axes)
    }

    override fun stopNestedScroll() {
        mChildHelper.stopNestedScroll()
    }

    override fun hasNestedScrollingParent(): Boolean {
        return mChildHelper.hasNestedScrollingParent()
    }

    override fun dispatchNestedScroll(dxConsumed: Int, dyConsumed: Int, dxUnconsumed: Int, dyUnconsumed: Int, offsetInWindow: IntArray?): Boolean {
        return mChildHelper.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow)
    }

    override fun dispatchNestedPreScroll(dx: Int, dy: Int, consumed: IntArray?, offsetInWindow: IntArray?): Boolean {
        return mChildHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow)
    }

    override fun dispatchNestedFling(velocityX: Float, velocityY: Float, consumed: Boolean): Boolean {
        return mChildHelper.dispatchNestedFling(velocityX, velocityY, consumed)
    }

    override fun dispatchNestedPreFling(velocityX: Float, velocityY: Float): Boolean {
        return mChildHelper.dispatchNestedPreFling(velocityX, velocityY)
    }

}

inline fun ViewManager.adblockWebView() = adblockWebView {}

inline fun ViewManager.adblockWebView(init: NestedScrollAdblockWebView.() -> Unit): NestedScrollAdblockWebView {
    return ankoView({ NestedScrollAdblockWebView(it) }, theme = 0, init = init)
}