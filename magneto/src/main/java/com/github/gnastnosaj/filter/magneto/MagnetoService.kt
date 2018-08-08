package com.github.gnastnosaj.filter.magneto

import groovy.lang.Script
import io.reactivex.Observable
import retrofit2.http.GET
import retrofit2.http.Path

interface MagnetoService {
    @GET("magneto/engines.json")
    fun engines(): Observable<List<MagnetoEngine>>

    @GET("magneto/{engine}.filter")
    fun engine(@Path("engine") engine: String): Observable<Script>
}