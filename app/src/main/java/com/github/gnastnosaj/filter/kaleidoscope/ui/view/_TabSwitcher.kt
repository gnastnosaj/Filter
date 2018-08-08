package com.github.gnastnosaj.filter.kaleidoscope.ui.view

import android.view.ViewManager
import de.mrapp.android.tabswitcher.TabSwitcher
import org.jetbrains.anko.custom.ankoView

inline fun ViewManager.tabSwitcher() = tabSwitcher {}

inline fun ViewManager.tabSwitcher(init: TabSwitcher.() -> Unit): TabSwitcher {
    return ankoView({ TabSwitcher(it) }, theme = 0, init = init)
}