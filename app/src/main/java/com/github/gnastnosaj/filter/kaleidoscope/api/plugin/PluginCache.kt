package com.github.gnastnosaj.filter.kaleidoscope.api.plugin

import com.github.gnastnosaj.filter.kaleidoscope.api.model.Plugin
import io.reactivex.Observable
import io.rx_cache2.EvictProvider
import io.rx_cache2.Expirable
import io.rx_cache2.ProviderKey

interface PluginCache {
    @Expirable(false)
    @ProviderKey("local")
    fun plugins(data: Observable<List<Plugin>>, evictProvider: EvictProvider): Observable<List<Plugin>>
}