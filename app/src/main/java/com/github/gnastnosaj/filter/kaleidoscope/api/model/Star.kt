package com.github.gnastnosaj.filter.kaleidoscope.api.model

class Star {
    var data = mutableMapOf<String, String>()

    override fun equals(other: Any?): Boolean {
        other?.let {
            if (it is Star) {
                return it.data["href"] == this.data["href"]
            }
        }
        return false
    }

    fun pure(): Map<String, String> {
        val pure = mutableMapOf<String, String>()
        pure.putAll(data)
        return pure
    }
}