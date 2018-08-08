package com.github.gnastnosaj.filter.kaleidoscope.api.event

import com.github.gnastnosaj.boilerplate.rxbus.RxBus


object ToolbarEvent {
    val observable = RxBus.getInstance().register(ToolbarEvent::class.java, ToolbarEvent::class.java)!!
}