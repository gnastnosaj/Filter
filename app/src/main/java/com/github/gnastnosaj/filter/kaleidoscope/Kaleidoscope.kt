package com.github.gnastnosaj.filter.kaleidoscope

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.Build.MANUFACTURER
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.KITKAT
import android.os.Build.VERSION_CODES.N
import android.support.multidex.MultiDex
import android.util.Log
import android.webkit.WebView
import com.github.gnastnosaj.boilerplate.Boilerplate
import com.github.gnastnosaj.boilerplate.event.ActivityLifecycleEvent
import com.github.gnastnosaj.boilerplate.rxbus.RxBus
import com.github.gnastnosaj.filter.kaleidoscope.net.OkHttpEnhancer.enhance
import com.github.gnastnosaj.filter.kaleidoscope.net.PluginInterceptor
import com.github.gnastnosaj.filter.kaleidoscope.util.ShareHelper
import com.github.piasy.biv.BigImageViewer
import com.github.piasy.biv.loader.fresco.FrescoImageLoader
import com.jiongbull.jlog.Logger
import com.jiongbull.jlog.constant.LogLevel
import com.jiongbull.jlog.constant.LogSegment
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import okhttp3.OkHttpClient
import okhttp3.OkUrlFactory
import org.adblockplus.libadblockplus.android.AdblockEngine
import org.adblockplus.libadblockplus.android.AndroidWebRequestResourceWrapper
import org.adblockplus.libadblockplus.android.settings.AdblockHelper
import timber.log.Timber
import java.io.File
import java.lang.ref.WeakReference
import java.net.URL

@Suppress("DEPRECATION")
class Kaleidoscope : Application() {
    private val instanceStatePool by lazy {
        HashMap<Int, Any>()
    }

    companion object {
        fun saveInstanceState(any: Any): Int {
            val pool = (Boilerplate.getInstance() as Kaleidoscope).instanceStatePool
            synchronized(pool) {
                pool.filterValues {
                    (it as WeakReference<*>).get() == null
                }.keys.forEach {
                    pool.remove(it)
                }

                val hashCode = any.hashCode()
                pool[hashCode] = WeakReference(any)
                return hashCode
            }
        }

        fun <T> restoreInstanceState(hashCode: Int): T? {
            val pool = (Boilerplate.getInstance() as Kaleidoscope).instanceStatePool
            synchronized(pool) {
                return (pool[hashCode] as? WeakReference<T>)?.get()
            }
        }
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        MultiDex.install(base)
        Boilerplate.runtime(false)
    }

    override fun onCreate() {
        super.onCreate()

        val logger = Logger.Builder.newBuilder(this, "Kaleidoscope")
                .setWriteToFile(true)
                .setLogDir("$packageName${File.separator}log")
                .setLogLevelsForFile(mutableListOf(LogLevel.WARN, LogLevel.ERROR, LogLevel.WTF))
                .setLogSegment(LogSegment.ONE_HOUR)
                .setPackagedLevel(6)
                .build()

        if (Boilerplate.initialize(this,
                        Boilerplate.Config.Builder()
                                .leakCanary(true)
                                .cockroach(true)
                                .addUncaughtExceptionHandler { _, e -> Timber.e(e) }
                                .logger { priority, tag, message, throwable ->
                                    when (priority) {
                                        Log.VERBOSE ->
                                            logger.v(tag, message)
                                        Log.DEBUG ->
                                            logger.d(tag, message)
                                        Log.INFO ->
                                            logger.i(tag, message)
                                        Log.WARN ->
                                            logger.w(tag, message)
                                        Log.ERROR ->
                                            logger.e(tag, throwable, message)
                                        Log.ASSERT ->
                                            logger.wtf(tag, throwable, message)
                                    }
                                }
                                .build()
                )) {
            logger.isDebug = Boilerplate.DEBUG

            val observable: Observable<ActivityLifecycleEvent> = RxBus.getInstance()
                    .register(ActivityLifecycleEvent::class.java, ActivityLifecycleEvent::class.java)

            var disposable: Disposable? = null
            disposable = observable.subscribe {
                if (MANUFACTURER == "samsung" && SDK_INT >= KITKAT && SDK_INT <= N) {
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
                WebView.setWebContentsDebuggingEnabled(Boilerplate.DEBUG)

                val okHttpClientBuilder = OkHttpClient.Builder()
                okHttpClientBuilder.apply {
                    enhance()
                    addInterceptor(PluginInterceptor)
                }

                URL.setURLStreamHandlerFactory(OkUrlFactory(okHttpClientBuilder.build()))
            }

            if (!AdblockHelper.get().isInit) {
                // init Adblock
                val basePath = getDir(AdblockEngine.BASE_PATH_DIRECTORY, Context.MODE_PRIVATE).absolutePath

                // provide preloaded subscriptions
                val map = java.util.HashMap<String, Int>()
                map[AndroidWebRequestResourceWrapper.EASYLIST] = R.raw.easylist
                map[AndroidWebRequestResourceWrapper.EASYLIST_CHINESE] = R.raw.easylistchina
                map[AndroidWebRequestResourceWrapper.ACCEPTABLE_ADS] = R.raw.exceptionrules

                AdblockHelper
                        .get()
                        .init(this, basePath, true, AdblockHelper.PREFERENCE_NAME)
                        .preloadSubscriptions(AdblockHelper.PRELOAD_PREFERENCE_NAME, map)
                        .addEngineCreatedListener {
                            Timber.d("adblock engine created")
                        }
                        .addEngineDisposedListener {
                            Timber.d("adblock engine disposed")
                        }
                        .retain(true)
            }

            BigImageViewer.initialize(FrescoImageLoader.with(this))

            ShareHelper.initialize(this)
        }
    }
}