package com.github.gnastnosaj.filter.dsl.groovy.api

import com.github.gnastnosaj.filter.dsl.groovy.util.DSLUtil
import org.jsoup.nodes.Document

class Page extends com.github.gnastnosaj.filter.dsl.core.Page {
    Document document

    Page() {}

    Page(Document document) {
        this.document = document
    }

    def document(Closure configureClosure) {
        return DSLUtil.configureObjectWithClosure(document, configureClosure, this)
    }

    def data(Closure configureClosure) {
        if (data == null) {
            data = new LinkedList<>()
        }
        return DSLUtil.configureObjectWithClosure(data, configureClosure)
    }

    def data(Map<String, Object> data) {
        if (this.data == null) {
            this.data = new LinkedList<>()
        }
        this.data.add(data)
        return this.data
    }

    def tags(Closure configureClosure) {
        if (tags == null) {
            tags = new LinkedHashMap<>()
        }
        return DSLUtil.configureObjectWithClosure(tags, configureClosure)
    }

    def tag(String name, Closure configureClosure) {
        if (tags == null) {
            tags = new LinkedHashMap<>()
        }
        if (tags.containsKey(name)) {
            DSLUtil.configureObjectWithClosure(tags.get(name), configureClosure)
            return tags.get(name)
        } else {
            Connection connection = new Connection()
            tags.put(name, connection)
            DSLUtil.configureObjectWithClosure(connection, configureClosure)
            return connection
        }
    }
}
