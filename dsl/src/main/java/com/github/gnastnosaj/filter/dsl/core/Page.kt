package com.github.gnastnosaj.filter.dsl.core

import org.jsoup.nodes.Document

abstract class Page(val document: Document) {
    var data: MutableList<MutableMap<String, String>>? = null
    var tags: MutableMap<String, Connection>? = null
}