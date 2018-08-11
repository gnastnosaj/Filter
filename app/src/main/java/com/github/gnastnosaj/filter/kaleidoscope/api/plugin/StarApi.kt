package com.github.gnastnosaj.filter.kaleidoscope.api.plugin

import android.content.Context
import com.github.gnastnosaj.boilerplate.Boilerplate
import com.github.gnastnosaj.filter.kaleidoscope.api.model.Plugin
import com.github.gnastnosaj.filter.kaleidoscope.api.model.Star
import io.reactivex.Observable
import io.rx_cache2.DynamicKey
import io.rx_cache2.EvictProvider
import io.rx_cache2.internal.RxCache
import io.victoralbertos.jolyglot.GsonSpeaker

class StarApi(private val plugin: Plugin) {
    companion object {
        private val cache: StarCache = RxCache.Builder()
                .persistence(Boilerplate.getInstance().getDir("starCache", Context.MODE_PRIVATE), GsonSpeaker())
                .using(StarCache::class.java)
    }

    fun stars(): Observable<List<Star>> {
        return cache.stars(Observable.just(mutableListOf()), DynamicKey(plugin.id), EvictProvider(false))
    }

    fun insertOrUpdate(vararg items: Star): Observable<List<Star>> {
        return cache.stars(stars().map {
            val data = mutableListOf<Star>()
            it.forEach {
                if (!items.contains(it)) {
                    data.add(it)
                }
            }
            return@map data + items
        }, DynamicKey(plugin.id), EvictProvider(true))
    }

    fun delete(vararg items: Star): Observable<List<Star>> {
        return cache.stars(stars().map {
            val data = mutableListOf<Star>()
            it.forEach {
                if (!items.contains(it)) {
                    data.add(it)
                }
            }
            return@map data
        }, DynamicKey(plugin.id), EvictProvider(true))
    }

    fun contains(item: Star): Observable<Boolean> {
        return stars().map {
            it.contains(item)
        }
    }
}