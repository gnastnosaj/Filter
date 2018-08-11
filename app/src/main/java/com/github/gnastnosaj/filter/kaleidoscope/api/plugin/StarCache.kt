package com.github.gnastnosaj.filter.kaleidoscope.api.plugin

import com.github.gnastnosaj.filter.kaleidoscope.api.model.Star
import io.reactivex.Observable
import io.rx_cache2.DynamicKey
import io.rx_cache2.EvictProvider
import io.rx_cache2.Expirable
import io.rx_cache2.ProviderKey

interface StarCache {
    @Expirable(false)
    @ProviderKey("star")
    fun stars(data: Observable<List<Star>>, dynamicKey: DynamicKey, evictProvider: EvictProvider): Observable<List<Star>>
}