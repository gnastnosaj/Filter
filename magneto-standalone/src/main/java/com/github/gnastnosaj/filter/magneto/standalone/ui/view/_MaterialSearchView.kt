package com.github.gnastnosaj.filter.magneto.standalone.ui.view

import android.view.ViewManager
import br.com.mauker.materialsearchview.MaterialSearchView
import org.jetbrains.anko.custom.ankoView

inline fun ViewManager.materialSearchView() = materialSearchView {}

inline fun ViewManager.materialSearchView(init: MaterialSearchView.() -> Unit): MaterialSearchView {
    return ankoView({ MaterialSearchView(it) }, theme = 0, init = init)
}