package com.github.gnastnosaj.filter.kaleidoscope.ui.view

import android.view.ViewManager
import com.airbnb.lottie.LottieAnimationView
import org.jetbrains.anko.custom.ankoView

inline fun ViewManager.lottieAnimationView(theme: Int = 0) = lottieAnimationView(theme) {}

inline fun ViewManager.lottieAnimationView(theme: Int = 0, init: LottieAnimationView.() -> Unit): LottieAnimationView {
    return ankoView({ LottieAnimationView(it) }, theme = theme, init = init)
}