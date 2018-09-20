package com.github.gnastnosaj.filter.kaleidoscope.api.plugin

import com.github.gnastnosaj.boilerplate.Boilerplate
import com.github.gnastnosaj.filter.dsl.groovy.GrooidClassLoader
import com.github.gnastnosaj.filter.dsl.groovy.api.Project
import com.github.gnastnosaj.filter.kaleidoscope.R
import com.github.gnastnosaj.filter.kaleidoscope.api.KaleidoscopeRetrofit
import com.github.gnastnosaj.filter.kaleidoscope.api.model.Plugin
import com.github.gnastnosaj.filter.kaleidoscope.net.PluginInterceptor
import groovy.lang.Script
import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import okhttp3.Request

object PluginApi {
    private var plugins: List<Plugin>? = null

    fun plugins(refresh: Boolean = true): Observable<List<Plugin>> {
        return if (refresh || plugins == null) {
            Observable
                    .zip(custom(), KaleidoscopeRetrofit.instance.service.plugins(), BiFunction<List<Plugin>, List<Plugin>, List<Plugin>> { custom, default -> custom + default })
                    .doOnNext {
                        PluginInterceptor.plugins(it)
                        plugins = it
                    }
        } else {
            Observable.just(plugins)
        }
    }

    fun initialize(plugin: Plugin): Observable<Project> {
        return Observable
                .just(plugin)
                .switchMap<Script> {
                    if (plugin.script != null) {
                        Observable
                                .create<Script> { emitter ->
                                    val request = Request.Builder().url(plugin.script!!).build()
                                    val call = KaleidoscopeRetrofit.instance.okHttpClient.newCall(request)
                                    val response = call.execute()
                                    response.body()?.let { body ->
                                        val script = GrooidClassLoader.loadAndCreateGroovyObject(Boilerplate.getInstance(), body.string()) as groovy.lang.Script
                                        emitter.onNext(script)
                                        emitter.onComplete()
                                    }
                                    emitter.onError(IllegalStateException())
                                }
                    } else {
                        KaleidoscopeRetrofit.instance.service.plugin(plugin.id!!)
                    }
                }
                .map { script ->
                    val project = Project(script)
                    project.execute("area", Boilerplate.getInstance().getString(R.string.area))
                    project.execute("build")
                    project
                }
    }

    private fun custom(): Observable<List<Plugin>> {
        return Observable.just(listOf())
    }
}