package com.github.gnastnosaj.filter.dsl.core

abstract class Connection {
    val tasks = mutableListOf<Task>()

    var url: String? = null
    var headers: MutableMap<String, String>? = null
    var cookies: MutableMap<String, String>? = null
    var data: MutableMap<String, String>? = null
    var timeout: Int? = null
    var followRedirects: Boolean = true
    var method: Method = Method.GET
    var layout: Layout = Layout.WATERFALL

    enum class Method {
        GET, POST
    }

    enum class Layout {
        WATERFALL, GALLERY
    }

    abstract fun execute(name: String, vararg args: Any): Any?

    abstract fun execute(name: String, arguments: Any): Any?

    abstract fun execute(name: String): Any?
}