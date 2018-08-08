package com.github.gnastnosaj.filter.magneto.standalone.api.datasource

import com.github.gnastnosaj.boilerplate.mvchelper.RxDataSource
import com.github.gnastnosaj.boilerplate.ui.activity.BaseActivity
import com.github.gnastnosaj.filter.magneto.standalone.api.SearchApi
import io.reactivex.Observable
import io.reactivex.ObservableTransformer

class MagnetoDataSource(private val baseActivity: BaseActivity) : RxDataSource<List<Map<String, String>>>(baseActivity) {
    var keyword: String? = null
    private var page: Int = 1

    private var preload: List<Map<String, String>>? = null
    private var hasMore = false

    override fun hasMore(): Boolean {
        return hasMore
    }

    override fun loadMore(): Observable<List<Map<String, String>>> {
        page++
        if (preload != null) {
            return Observable.just(preload!!).compose(preloadTransformer)
        }
        throw IllegalStateException()
    }

    override fun refresh(): Observable<List<Map<String, String>>> {
        page = 1
        if (!keyword.isNullOrBlank()) {
            return SearchApi.search(baseActivity, keyword!!).compose(preloadTransformer)
        }
        throw IllegalStateException()
    }

    private var preloadTransformer = ObservableTransformer<List<Map<String, String>>, List<Map<String, String>>> { upstream ->
        upstream.flatMap { data ->
            SearchApi.index(baseActivity, keyword!!, page + 1)
                    .onErrorResumeNext { _: Throwable ->
                        Observable.just(listOf())
                    }
                    .doOnNext {
                        hasMore = it.isNotEmpty()

                        if (hasMore) {
                            preload = it
                        }
                    }
                    .map {
                        data
                    }
        }
    }
}