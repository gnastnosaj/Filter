package com.github.gnastnosaj.filter.dsl.core

abstract class Catalog {
    var connections: MutableMap<String, Connection>? = null
    var categories: MutableList<Category>? = null
}