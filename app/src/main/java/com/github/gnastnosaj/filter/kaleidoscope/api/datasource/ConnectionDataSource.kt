package com.github.gnastnosaj.filter.kaleidoscope.api.datasource

import android.content.Context
import com.github.gnastnosaj.boilerplate.mvchelper.RxDataSource
import com.github.gnastnosaj.boilerplate.rxbus.RxBus
import com.github.gnastnosaj.filter.dsl.core.Connection
import com.github.gnastnosaj.filter.dsl.core.Page
import com.github.gnastnosaj.filter.kaleidoscope.api.event.TagEvent
import io.reactivex.Observable

class ConnectionDataSource(context: Context, private val connection: Connection, private var preload: Page? = null) : RxDataSource<List<Map<String, String>>>(context) {
    override fun hasMore(): Boolean {
        return connection.execute("hasMore") as? Boolean ?: false
    }

    override fun loadMore(): Observable<List<Map<String, String>>> {
        return Observable.create<List<Map<String, String>>> {
            (connection.execute("loadMore") as? Page)?.let { page ->
                it.onNext(page.data ?: listOf())
                it.onComplete()
                return@create
            }
            throw IllegalStateException()
        }
    }

    override fun refresh(): Observable<List<Map<String, String>>> {
        return Observable.create<List<Map<String, String>>> {
            if (preload != null && preload?.data?.isNotEmpty() == true) {
                it.onNext(preload?.data ?: listOf())
                preload = null
                it.onComplete()
                return@create
            }
            (connection.execute("refresh") as? Page)?.let { page ->
                page.tags?.let { tags ->
                    RxBus.getInstance().post(connection, TagEvent(tags))
                }
                it.onNext(page.data ?: listOf())
                it.onComplete()
                return@create
            }
            throw IllegalStateException()
        }.retry(3)
    }
}

