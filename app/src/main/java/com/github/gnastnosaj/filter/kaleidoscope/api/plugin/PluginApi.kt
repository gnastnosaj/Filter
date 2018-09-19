package com.github.gnastnosaj.filter.kaleidoscope.api.plugin

import com.github.gnastnosaj.filter.kaleidoscope.api.KaleidoscopeRetrofit
import com.github.gnastnosaj.filter.kaleidoscope.api.model.Plugin
import com.github.gnastnosaj.filter.kaleidoscope.net.PluginInterceptor
import io.reactivex.Observable
import io.reactivex.functions.BiFunction

class PluginApi {
    companion object {
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

        private fun custom(): Observable<List<Plugin>> {
            return Observable.just(listOf())
        }
    }
}