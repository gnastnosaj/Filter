package com.github.gnastnosaj.filter.kaleidoscope.api.event

import com.github.gnastnosaj.boilerplate.rxbus.RxBus

class PreviewEvent(val type: Int) {
    companion object {
        const val TYPE_NEXT = 0
        const val TYPE_PRE = 1

        val observable = RxBus.getInstance().register(PreviewEvent::class.java, PreviewEvent::class.java)!!
    }
}