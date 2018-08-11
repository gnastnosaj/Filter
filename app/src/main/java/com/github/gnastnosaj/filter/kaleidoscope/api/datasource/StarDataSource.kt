package com.github.gnastnosaj.filter.kaleidoscope.api.datasource

import android.content.Context
import com.github.gnastnosaj.boilerplate.mvchelper.RxDataSource
import com.github.gnastnosaj.filter.kaleidoscope.api.model.Plugin
import com.github.gnastnosaj.filter.kaleidoscope.api.plugin.StarApi
import io.reactivex.Observable

class StarDataSource(context: Context, plugin: Plugin) : RxDataSource<List<Map<String, String>>>(context) {
    private val starApi = StarApi(plugin)

    override fun hasMore(): Boolean {
        return false
    }

    override fun loadMore(): Observable<List<Map<String, String>>> {
        return Observable.empty()
    }

    override fun refresh(): Observable<List<Map<String, String>>> {
        return starApi.stars().map {
            it.map {
                it.pure()
            }
        }
    }
}

