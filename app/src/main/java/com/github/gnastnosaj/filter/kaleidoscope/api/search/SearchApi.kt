package com.github.gnastnosaj.filter.kaleidoscope.api.search

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.support.design.widget.Snackbar
import com.github.gnastnosaj.boilerplate.Boilerplate
import com.github.gnastnosaj.boilerplate.rxbus.RxHelper
import com.github.gnastnosaj.boilerplate.ui.activity.BaseActivity
import com.github.gnastnosaj.filter.kaleidoscope.R
import com.github.gnastnosaj.filter.kaleidoscope.ui.activity.MagnetoActivity
import com.github.gnastnosaj.filter.magneto.Magneto
import com.tbruyelle.rxpermissions2.RxPermissions
import com.trello.rxlifecycle2.android.ActivityEvent
import io.reactivex.Observable
import io.rx_cache2.DynamicKeyGroup
import io.rx_cache2.internal.RxCache
import io.victoralbertos.jolyglot.GsonSpeaker


class SearchApi {
    companion object {
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
                                Magneto.newInstance(baseActivity).doOnNext {
                                    magneto = it
                                }
                            } else {
                                Observable.just(magneto)
                            }
                        } else {
                            throw IllegalStateException()
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
}

fun BaseActivity.search(keyword: String, title: String = keyword): Observable<List<Map<String, String>>> {
    return Observable
            .defer {
                Snackbar.make(findViewById(android.R.id.content), R.string.searching, Snackbar.LENGTH_LONG).show()
                SearchApi.search(this, keyword).compose(RxHelper.rxSchedulerHelper())
            }
            .doOnNext {
                if (it.isNotEmpty()) {
                    AlertDialog.Builder(this)
                            .setMessage(R.string.search_result_found)
                            .setNegativeButton(R.string.action_cancel) { dialog, _ -> dialog.dismiss() }
                            .setPositiveButton(R.string.action_check) { dialog, _ ->
                                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("magneto:$keyword")).putExtra(MagnetoActivity.EXTRA_TITLE, title))
                                dialog.dismiss()
                            }.show()
                } else {
                    Snackbar.make(findViewById(android.R.id.content), R.string.search_result_not_found, Snackbar.LENGTH_LONG).show()
                }
            }
            .doOnError {
                Snackbar.make(findViewById(android.R.id.content), R.string.search_result_not_found, Snackbar.LENGTH_LONG).show()
            }
            .compose(bindUntilEvent(ActivityEvent.DESTROY))
}