package com.github.gnastnosaj.filter.kaleidoscope.ui.view

import android.view.ViewManager
import org.jetbrains.anko.custom.ankoView

inline fun ViewManager.mosaicView() = mosaicView {}

inline fun ViewManager.mosaicView(init: MosaicView.() -> Unit): MosaicView {
    return ankoView({ MosaicView(it) }, theme = 0, init = init)
}