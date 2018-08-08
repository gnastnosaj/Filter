package com.github.gnastnosaj.filter.kaleidoscope.api.plugin

import android.content.Context
import com.github.gnastnosaj.boilerplate.Boilerplate
import com.github.gnastnosaj.filter.kaleidoscope.api.KaleidoscopeRetrofit
import com.github.gnastnosaj.filter.kaleidoscope.api.model.Plugin
import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import io.rx_cache2.EvictProvider
import io.rx_cache2.internal.RxCache
import io.victoralbertos.jolyglot.GsonSpeaker

class PluginApi {
    companion object {
        private val cache: PluginCache = RxCache.Builder()
                .persistence(Boilerplate.getInstance().getDir("pluginCache", Context.MODE_PRIVATE), GsonSpeaker())
                .using(PluginCache::class.java)

        private var plugins: List<Plugin>? = null

        fun plugins(refresh: Boolean = true): Observable<List<Plugin>> {
            return if (refresh || plugins == null) {
                Observable
                        .zip(local(), remote(), BiFunction<List<Plugin>, List<Plugin>, List<Plugin>> { local, remote -> local + remote })
                        .doOnNext {
                            plugins = it
                        }
            } else {
                Observable.just(plugins)
            }
        }

        fun remote(): Observable<List<Plugin>> {
            return KaleidoscopeRetrofit.instance.service.plugins()
        }

        fun local(): Observable<List<Plugin>> {
            return cache.plugins(Observable.just(mutableListOf()), EvictProvider(false))
        }

        fun insertOrUpdate(vararg plugins: Plugin): Observable<List<Plugin>> {
            return cache.plugins(local().map {
                val data = mutableListOf<Plugin>()
                it.forEach {
                    if (!plugins.contains(it)) {
                        data.add(it)
                    }
                }
                return@map data + plugins
            }, EvictProvider(true))
        }

        fun delete(vararg plugins: Plugin): Observable<List<Plugin>> {
            return cache.plugins(local().map {
                val data = mutableListOf<Plugin>()
                it.forEach {
                    if (!plugins.contains(it)) {
                        data.add(it)
                    }
                }
                return@map data
            }, EvictProvider(true))
        }
    }
}