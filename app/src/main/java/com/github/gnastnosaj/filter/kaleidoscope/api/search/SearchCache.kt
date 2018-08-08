package com.github.gnastnosaj.filter.kaleidoscope.api.search

import io.reactivex.Observable
import io.rx_cache2.DynamicKeyGroup
import io.rx_cache2.LifeCache
import io.rx_cache2.ProviderKey
import java.util.concurrent.TimeUnit

interface SearchCache {
    @ProviderKey("search")
    @LifeCache(duration = 1, timeUnit = TimeUnit.HOURS)
    fun search(data: Observable<List<Map<String, String>>>, dynamicKeyGroup: DynamicKeyGroup): Observable<List<Map<String, String>>>
}