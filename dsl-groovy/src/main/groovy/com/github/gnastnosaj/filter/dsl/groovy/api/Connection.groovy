package com.github.gnastnosaj.filter.dsl.groovy.api

import com.github.gnastnosaj.filter.dsl.groovy.util.DSLUtil
import org.jsoup.Jsoup

class Connection extends com.github.gnastnosaj.filter.dsl.core.Connection {

    def url(String url) {
        this.url = url
    }

    def headers(Map<String, String> headers) {
        if (this.headers == null) {
            this.headers = new LinkedHashMap<>()
        }
        this.headers.putAll(headers)
    }

    def headers(String key, String value) {
        if (this.headers == null) {
            this.headers = new LinkedHashMap<>()
        }
        this.headers[key] = value
    }

    def cookies(Map<String, String> cookies) {
        if (this.cookies == null) {
            this.cookies = new LinkedHashMap<>()
        }
        this.cookies.putAll(cookies)
    }

    def cookies(String key, String value) {
        if (this.cookies == null) {
            this.cookies = new LinkedHashMap<>()
        }
        this.cookies[key] = value
    }

    def data(Map<String, String> data) {
        if (this.data == null) {
            this.data = new LinkedHashMap<>()
        }
        this.data.putAll(data)
    }

    def data(String key, String value) {
        if (this.data == null) {
            this.data = new LinkedHashMap<>()
        }
        this.data[key] = value
    }

    def timeout(int timeout) {
        this.timeout = timeout
    }

    def followRedirects(boolean followRedirects) {
        this.followRedirects = followRedirects
    }

    def method(com.github.gnastnosaj.filter.dsl.core.Connection.Method method) {
        this.method = method
    }

    def layout(com.github.gnastnosaj.filter.dsl.core.Connection.Layout layout) {
        this.layout = layout
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

    def blank(Closure configureClosure) {
        def page = new Page()
        DSLUtil.configureObjectWithClosure(page, configureClosure)
        return page
    }

    def page(Closure configureClosure) {
        def page

        if (url != null) {
            org.jsoup.Connection connection = Jsoup.connect(url)

            if (headers != null) {
                connection.headers(headers)
            }
            if (cookies != null) {
                connection.cookies(cookies)
            }
            if (data != null) {
                connection.data(data)
            }
            if (timeout != null) {
                connection.timeout(timeout)
            }

            connection.followRedirects(followRedirects)

            if (method == com.github.gnastnosaj.filter.dsl.core.Connection.Method.GET) {
                page = new Page(connection.get())
            } else {
                page = new Page(connection.post())
            }

            DSLUtil.configureObjectWithClosure(page, configureClosure)
        }

        return page
    }

    def raw(Closure configureClosure) {
        def raw

        if (url != null) {
            org.jsoup.Connection connection = Jsoup.connect(url)

            if (headers != null) {
                connection.headers(headers)
            }
            if (cookies != null) {
                connection.cookies(cookies)
            }
            if (data != null) {
                connection.data(data)
            }
            if (timeout != null) {
                connection.timeout(timeout)
            }

            connection.followRedirects(followRedirects)
            connection.ignoreContentType(true)

            if (method == com.github.gnastnosaj.filter.dsl.core.Connection.Method.GET) {
                connection.request().method(org.jsoup.Connection.Method.GET)
            } else {
                connection.request().method(org.jsoup.Connection.Method.POST)
            }

            raw = new Raw(connection.execute().body())

            DSLUtil.configureObjectWithClosure(raw, configureClosure)
        }

        return raw
    }
}
