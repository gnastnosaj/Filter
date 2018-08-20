package com.github.gnastnosaj.filter.magneto.standalone

import android.app.Application
import android.content.Context
import android.os.Build
import android.support.multidex.MultiDex
import com.github.gnastnosaj.boilerplate.Boilerplate
import okhttp3.OkHttpClient
import okhttp3.OkUrlFactory
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.HttpLoggingInterceptor.Level
import timber.log.Timber
import java.net.URL

class Magneto : Application() {
    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        MultiDex.install(base)
        Boilerplate.runtime(false)
    }

    override fun onCreate() {
        super.onCreate()
        if (Boilerplate.initialize(this, Boilerplate.Config.Builder().fresco(false).cockroach(true).addUncaughtExceptionHandler { _, e -> Timber.e(e) }.build())) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                val logging = HttpLoggingInterceptor()
                logging.level = Level.BASIC

                val okHttpClient = OkHttpClient.Builder()
                        .addNetworkInterceptor(logging)
                        .build()

                URL.setURLStreamHandlerFactory(OkUrlFactory(okHttpClient))
            }
        }
    }
}