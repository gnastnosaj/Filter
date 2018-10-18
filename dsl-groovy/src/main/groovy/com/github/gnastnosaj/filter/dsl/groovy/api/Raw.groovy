/*
 * Copyright (c) 2018, Jason Tsang.(https://github.com/gnastnosaj) All Rights Reserved.
 */

package com.github.gnastnosaj.filter.dsl.groovy.api

import com.github.gnastnosaj.filter.dsl.groovy.util.DSLUtil
import org.json.JSONObject

class Raw extends com.github.gnastnosaj.filter.dsl.core.Page {
    String raw

    Raw() {}

    Raw(String raw) {
        this.raw = raw
    }

    def json(Closure configureClosure) {
        def json = new JSONObject(raw)
        return DSLUtil.configureObjectWithClosure(json, configureClosure, this)
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
