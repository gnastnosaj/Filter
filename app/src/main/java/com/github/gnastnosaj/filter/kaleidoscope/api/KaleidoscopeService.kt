package com.github.gnastnosaj.filter.kaleidoscope.api

import com.github.gnastnosaj.filter.kaleidoscope.api.model.Plugin
import groovy.lang.Script
import io.reactivex.Observable
import retrofit2.http.GET
import retrofit2.http.Path

interface KaleidoscopeService {
    @GET("kaleidoscope/plugins.json")
    fun plugins(): Observable<List<Plugin>>

    @GET("kaleidoscope/{id}.filter")
    fun plugin(@Path("id") id: String): Observable<Script>
}