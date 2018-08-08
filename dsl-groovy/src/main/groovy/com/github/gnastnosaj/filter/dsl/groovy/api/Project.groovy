package com.github.gnastnosaj.filter.dsl.groovy.api

import com.github.gnastnosaj.filter.dsl.groovy.util.DSLUtil

class Project extends com.github.gnastnosaj.filter.dsl.core.Project {

    Project(Script script) {
        super()
        DSLUtil.setDelegateForGroovyObject(script, this)
        script.run()
    }

    def catalog(Closure configureClosure) {
        if (catalog == null) {
            catalog = new Catalog()
        }
        return DSLUtil.configureObjectWithClosure(catalog, configureClosure)
    }

    def connection(Closure configureClosure) {
        def connection = new Connection()
        DSLUtil.configureObjectWithClosure(connection, configureClosure)
        return connection
    }

    def task(String name, Closure configureClosure) {
        for (com.github.gnastnosaj.filter.dsl.core.Task task : tasks) {
            if (task.name == name) {
                ((Task) task).action(configureClosure)
                return task
            }
        }
        Task task = new Task(name, this)
        task.action(configureClosure)
        tasks.add(task)
        return task
    }

    def execute(String name, Object... args) {
        for (com.github.gnastnosaj.filter.dsl.core.Task task : tasks) {
            if (task.name == name) {
                return ((Task) task).execute(args)
            }
        }
    }

    def execute(String name, Object arguments) {
        for (com.github.gnastnosaj.filter.dsl.core.Task task : tasks) {
            if (task.name == name) {
                return ((Task) task).execute(arguments)
            }
        }
    }

    def execute(String name) {
        return execute(name, null)
    }
}
