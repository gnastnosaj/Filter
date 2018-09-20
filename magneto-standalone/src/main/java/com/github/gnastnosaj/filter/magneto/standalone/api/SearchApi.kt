package com.github.gnastnosaj.filter.magneto.standalone.api

import android.Manifest
import android.content.Context
import com.github.gnastnosaj.boilerplate.Boilerplate
import com.github.gnastnosaj.boilerplate.ui.activity.BaseActivity
import com.github.gnastnosaj.filter.magneto.Magneto
import com.tbruyelle.rxpermissions2.RxPermissions
import io.reactivex.Observable
import io.rx_cache2.DynamicKeyGroup
import io.rx_cache2.internal.RxCache
import io.victoralbertos.jolyglot.GsonSpeaker


object SearchApi {
    private val cache: SearchCache = RxCache.Builder()
            .persistence(Boilerplate.getInstance().getDir("searchCache", Context.MODE_PRIVATE), GsonSpeaker())
            .using(SearchCache::class.java)

    private var magneto: Magneto? = null

    private fun engine(baseActivity: BaseActivity): Observable<Magneto> {
        return RxPermissions(baseActivity)
                .request(Manifest.permission.INTERNET, Manifest.permission.ACCESS_NETWORK_STATE)
                .switchMap { granted ->
                    if (granted) {
                        if (magneto == null) {
                            Magneto.newInstance(Boilerplate.getInstance()).doOnNext {
                                magneto = it
                            }
                        } else {
                            Observable.just(magneto)
                        }
                    } else {
                        throw IllegalStateException("permission denied")
                    }
                }
    }

    fun search(baseActivity: BaseActivity, keyword: String): Observable<List<Map<String, String>>> {
        return cache.search(engine(baseActivity)
                .flatMap { magneto ->
                    magneto.search(keyword)
                }, DynamicKeyGroup(1, keyword))
    }

    fun index(baseActivity: BaseActivity, keyword: String, page: Int): Observable<List<Map<String, String>>> {
        return cache.search(engine(baseActivity)
                .flatMap { magneto ->
                    magneto.index(keyword, page)
                }, DynamicKeyGroup(page, keyword))
    }
}