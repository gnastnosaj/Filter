package com.github.gnastnosaj.filter.kaleidoscope.api.plugin

import android.content.Context
import com.github.gnastnosaj.boilerplate.Boilerplate
import com.github.gnastnosaj.filter.dsl.groovy.GrooidClassLoader
import com.github.gnastnosaj.filter.dsl.groovy.api.Project
import com.github.gnastnosaj.filter.kaleidoscope.R
import com.github.gnastnosaj.filter.kaleidoscope.api.KaleidoscopeRetrofit
import com.github.gnastnosaj.filter.kaleidoscope.api.model.Plugin
import com.github.gnastnosaj.filter.kaleidoscope.net.PluginInterceptor
import com.google.gson.Gson
import groovy.lang.Script
import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import okhttp3.Request

object PluginApi {
    const val PREF_REPOSITORIES = "repositories"

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
        val repositories = Boilerplate.getInstance().getSharedPreferences(PREF_REPOSITORIES, Context.MODE_PRIVATE).getStringSet(PREF_REPOSITORIES, setOf())
        if (repositories.isEmpty()) {
            return Observable.just(listOf())
        } else {
            return Observable
                    .zip(
                            repositories.map {
                                Observable
                                        .create<List<Plugin>> { emitter ->
                                            val request = Request.Builder().url(it).build()
                                            val call = KaleidoscopeRetrofit.instance.okHttpClient.newCall(request)
                                            val response = call.execute()
                                            response.body()?.let {
                                                val plugins = Gson().fromJson<List<Plugin>>(it.string(), List::class.java)
                                                emitter.onNext(plugins)
                                                emitter.onComplete()
                                                return@create
                                            }
                                            emitter.onError(IllegalStateException())
                                        }
                                        .onErrorReturn {
                                            listOf()
                                        }
                            }
                    ) {
                        val plugins = mutableListOf<Plugin>()
                        it.forEach {
                            plugins.addAll(it as List<Plugin>)
                        }
                        return@zip plugins
                    }
        }
    }
}