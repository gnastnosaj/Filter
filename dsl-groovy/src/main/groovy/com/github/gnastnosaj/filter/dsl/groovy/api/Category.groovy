package com.github.gnastnosaj.filter.dsl.groovy.api

import com.github.gnastnosaj.filter.dsl.groovy.util.DSLUtil

class Category extends com.github.gnastnosaj.filter.dsl.core.Category {
    Category(String name) {
        super(name)
    }

    def children(String name, Closure configureClosure) {
        if (children == null) {
            children = new LinkedList<>()
        }
        for (category in children) {
            if (category.name == name) {
                DSLUtil.configureObjectWithClosure(category, configureClosure)
                return category
            }
        }
        def category = new Category(name)
        children.add(category)
        DSLUtil.configureObjectWithClosure(category, configureClosure)
        return category
    }

    def connection(Closure configureClosure) {
        if (connection == null) {
            connection = new Connection()
        }
        DSLUtil.configureObjectWithClosure(connection, configureClosure)
        return connection
    }
}
