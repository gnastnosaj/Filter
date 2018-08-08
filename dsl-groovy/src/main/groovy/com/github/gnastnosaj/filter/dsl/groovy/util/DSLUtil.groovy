package com.github.gnastnosaj.filter.dsl.groovy.util

class DSLUtil {

    static void setDelegateForGroovyObject(GroovyObject obj, Object delegate) {
        obj.metaClass.getProperty = { name ->
            def metaProperty = obj.metaClass.getMetaProperty(name)
            metaProperty != null ? metaProperty : delegate.getProperty(name)
        }

        obj.metaClass.invokeMethod = { name, args ->
            def metaMethod = obj.metaClass.getMetaMethod(name, args)
            metaMethod != null ? metaMethod.invoke(obj, args) : delegate.invokeMethod(name, args)
        }
    }

    static Object runClosureAgainstObject(Closure closure, Object delegate) {
        Closure c = (Closure) closure.clone()
        c.delegate = delegate
        c.call()
    }

    static Object runClosureAgainstObject(Closure closure, Object delegate, Object... args) {
        Closure c = (Closure) closure.clone()
        c.delegate = delegate
        c.call(args)
    }

    static Object runClosureAgainstObject(Closure closure, Object delegate, Object arguments) {
        Closure c = (Closure) closure.clone()
        c.delegate = delegate
        c.call(arguments)
    }

    static Object configureObjectWithClosure(Object object, Closure closure) {
        Closure c = (Closure) closure.clone()
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c.delegate = object
        c.call()
    }

    static Object configureObjectWithClosure(Object object, Closure closure, Object... args) {
        Closure c = (Closure) closure.clone()
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c.delegate = object
        c.call(args)
    }

    static Object configureObjectWithClosure(Object object, Closure closure, Object arguments) {
        Closure c = (Closure) closure.clone()
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c.delegate = object
        c.call(arguments)
    }
}