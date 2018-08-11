package com.github.gnastnosaj.filter.kaleidoscope.api.model

class Star {
    var href: String? = null
    var data = mutableMapOf<String, String>()

    override fun equals(other: Any?): Boolean {
        other?.let {
            if (it is Star) {
                return it.href == this.href
            }
        }
        return false
    }

    fun pure(): Map<String, String> {
        val pure = mutableMapOf<String, String>()
        href?.let {
            pure["href"] = it
        }
        pure.putAll(data)
        return pure
    }
}