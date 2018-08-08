package com.github.gnastnosaj.filter.kaleidoscope.ui.view

import android.view.ViewManager
import me.gujun.android.taggroup.TagGroup
import org.jetbrains.anko.custom.ankoView

inline fun ViewManager.tagGroup(theme: Int = 0) = tagGroup(theme) {}

inline fun ViewManager.tagGroup(theme: Int = 0, init: TagGroup.() -> Unit): TagGroup {
    return ankoView({ TagGroup(it) }, theme = theme, init = init)
}