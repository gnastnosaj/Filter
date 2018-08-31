package com.github.gnastnosaj.filter.kaleidoscope.ui.view

import android.view.ViewManager
import developer.shivam.crescento.CrescentoContainer
import org.jetbrains.anko.custom.ankoView

inline fun ViewManager.crescentoContainer() = crescentoContainer {}

inline fun ViewManager.crescentoContainer(init: CrescentoContainer.() -> Unit): CrescentoContainer {
    return ankoView({ CrescentoContainer(it) }, theme = 0, init = init)
}