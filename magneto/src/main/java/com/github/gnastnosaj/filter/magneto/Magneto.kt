package com.github.gnastnosaj.filter.magneto

import android.content.Context
import com.github.gnastnosaj.filter.dsl.core.Page
import com.github.gnastnosaj.filter.dsl.groovy.api.Project
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers

class Magneto private constructor(context: Context) {
    private val retrofit: MagnetoRetrofit = MagnetoRetrofit(context)
    private val projects = mutableMapOf<MagnetoEngine, Project>()

    private var engine: MagnetoEngine? = null

    companion object {
        fun newInstance(context: Context): Observable<Magneto> {
            return Observable.create<Magneto> { emitter ->
                val magneto = Magneto(context)

                magneto.retrofit.service.engines()
                        .subscribeOn(Schedulers.io())
                        .flatMap { engines ->
                            Observable.fromIterable(engines)
                        }.flatMap { engine ->
                            magneto.retrofit.service.engine(engine.id)
                                    .map { script ->
                                        val project = Project(script)
                                        magneto.projects[engine] = project
                                        project
                                    }.onErrorResumeNext { _: Throwable ->
                                        Observable.empty()
                                    }
                        }.subscribe({
                        }, { throwable ->
                            emitter.onError(throwable)
                        }, {
                            emitter.onNext(magneto)
                            emitter.onComplete()
                        })
            }
        }
    }

    fun search(keyword: String): Observable<List<Map<String, String>>> {
        engine = projects.entries.firstOrNull()?.key
        return internalSearch(keyword).switchMap { result ->
            if (result.isEmpty() && switch()) {
                internalSearch(keyword)
            } else {
                Observable.just(result)
            }
        }.onErrorResumeNext { throwable: Throwable ->
            if (switch()) {
                internalSearch(keyword)
            } else {
                throw throwable
            }
        }
    }

    fun index(keyword: String, page: Int): Observable<List<Map<String, String>>> {
        return Observable.create<List<Map<String, String>>> { emitter ->
            engine?.let { engine ->
                projects[engine]?.execute("index", keyword, page)?.let { page ->
                    (page as? Page)?.let { page ->
                        emitter.onNext(page.data ?: listOf())
                        emitter.onComplete()
                        return@create
                    }
                }
            }
            emitter.onError(IllegalStateException())
        }
    }

    private fun switch(): Boolean {
        val engines = projects.keys.toList()
        for (i in 0 until engines.size) {
            if (engines[i] == engine && i < engines.size - 1) {
                engine = engines[i + 1]
                return true
            }
        }
        return false
    }

    private fun internalSearch(keyword: String): Observable<List<Map<String, String>>> {
        return Observable.create<List<Map<String, String>>> { emitter ->
            projects[engine]?.execute("search", keyword)?.let { page ->
                (page as? Page)?.let { page ->
                    emitter.onNext(page.data ?: listOf())
                    emitter.onComplete()
                    return@create
                }
            }
            emitter.onError(IllegalStateException())
        }
    }
}