package com.github.gnastnosaj.filter.dsl.core

abstract class Project {
    val tasks = mutableListOf<Task>()
    var catalog: Catalog? = null

    abstract fun execute(name: String, vararg args: Any): Any?

    abstract fun execute(name: String, arguments: Any): Any?

    abstract fun execute(name: String): Any?
}