package com.github.gnastnosaj.filter.magneto.standalone

import android.app.Application
import android.content.Context
import android.os.Build
import android.support.multidex.MultiDex
import com.github.gnastnosaj.boilerplate.Boilerplate
import com.github.gnastnosaj.boilerplate.event.ActivityLifecycleEvent
import com.github.gnastnosaj.boilerplate.rxbus.RxBus
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
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
            val observable: Observable<ActivityLifecycleEvent> = RxBus.getInstance()
                    .register(ActivityLifecycleEvent::class.java, ActivityLifecycleEvent::class.java)

            var disposable: Disposable? = null
            disposable = observable.subscribe {
                if (Build.MANUFACTURER == "samsung" && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && Build.VERSION.SDK_INT <= Build.VERSION_CODES.N) {
                    if (it.type == ActivityLifecycleEvent.onActivityDestroyed) {
                        try {
                            val semEmergencyManagerClass = Class.forName("com.samsung.android.emergencymode.SemEmergencyManager")
                            val sInstanceField = semEmergencyManagerClass.getDeclaredField("sInstance")
                            sInstanceField.isAccessible = true
                            val sInstance = sInstanceField.get(null)
                            val mContextField = semEmergencyManagerClass.getDeclaredField("mContext")
                            mContextField.isAccessible = true
                            mContextField.set(sInstance, this)
                        } catch (_: Throwable) {
                        }
                        RxBus.getInstance().unregister(ActivityLifecycleEvent::class.java, observable)
                        disposable?.apply {
                            if (!isDisposed) {
                                dispose()
                            }
                        }
                        Timber.d("Fixes a leak caused by SemEmergencyManager. Tracked at https://github.com/square/leakcanary/issues/762")
                    }
                }
            }

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