package com.github.gnastnosaj.filter.dsl.core

abstract class Page {
    var data: MutableList<MutableMap<String, String>>? = null
    var tags: MutableMap<String, Connection>? = null
}