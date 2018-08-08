package com.github.gnastnosaj.filter.dsl.groovy.api

import com.github.gnastnosaj.filter.dsl.groovy.util.DSLUtil

class Task extends com.github.gnastnosaj.filter.dsl.core.Task {
    Closure action

    Task(String name, Object context) {
        super(name, context)
    }

    def action(Closure configureClosure) {
        action = configureClosure
    }

    def execute(Object... args) {
        return DSLUtil.configureObjectWithClosure(context, action, args)
    }

    def execute(Object arguments) {
        return DSLUtil.configureObjectWithClosure(context, action, arguments)
    }
}
