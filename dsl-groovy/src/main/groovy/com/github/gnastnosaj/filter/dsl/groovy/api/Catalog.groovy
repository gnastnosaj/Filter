package com.github.gnastnosaj.filter.dsl.groovy.api

import com.github.gnastnosaj.filter.dsl.groovy.util.DSLUtil

class Catalog extends com.github.gnastnosaj.filter.dsl.core.Catalog {

    def connection(String name, Closure configureClosure) {
        if (connections == null) {
            connections = new LinkedHashMap<>()
        }
        Connection connection
        if (connections.containsKey(name)) {
            connection = connections.get(name)
        } else {
            connection = new Connection()
            connections.put(name, connection)
        }
        DSLUtil.configureObjectWithClosure(connection, configureClosure)
        return connection
    }

    def category(String name, Closure configureClosure) {
        if (categories == null) {
            categories = new LinkedList<>()
        }
        for (category in categories) {
            if (category.name == name) {
                DSLUtil.configureObjectWithClosure(category, configureClosure)
                return category
            }
        }
        Category category = new Category(name)
        categories.add(category)
        DSLUtil.configureObjectWithClosure(category, configureClosure)
        return category
    }
}
