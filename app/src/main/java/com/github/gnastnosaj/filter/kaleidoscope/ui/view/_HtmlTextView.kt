/*
 * Copyright (c) 2018, Jason Tsang.(https://github.com/gnastnosaj) All Rights Reserved.
 */

package com.github.gnastnosaj.filter.kaleidoscope.ui.view

import android.view.ViewManager
import org.jetbrains.anko.custom.ankoView
import org.sufficientlysecure.htmltextview.HtmlTextView

inline fun ViewManager.htmlTextView() = htmlTextView {}

inline fun ViewManager.htmlTextView(init: HtmlTextView.() -> Unit): HtmlTextView {
    return ankoView({ HtmlTextView(it) }, theme = 0, init = init)
}