package com.github.gnastnosaj.filter.dsl.core

abstract class Category(val name: String) {
    var connection: Connection? = null
    var children: MutableList<Category>? = null
}