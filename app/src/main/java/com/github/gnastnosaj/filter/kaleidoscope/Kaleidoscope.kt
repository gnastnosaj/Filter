package com.github.gnastnosaj.filter.kaleidoscope

import android.app.Activity
import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import android.os.StrictMode
import android.support.multidex.MultiDex
import android.util.Log
import android.webkit.WebView
import com.facebook.common.memory.MemoryTrimType
import com.facebook.common.memory.MemoryTrimmable
import com.facebook.common.memory.MemoryTrimmableRegistry
import com.facebook.imagepipeline.core.ImagePipelineConfig
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
import ezy.boost.update.UpdateManager
import io.reactivex.Completable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import okhttp3.OkHttpClient
import okhttp3.OkUrlFactory
import org.adblockplus.libadblockplus.android.AdblockEngine
import org.adblockplus.libadblockplus.android.AndroidWebRequestResourceWrapper
import org.adblockplus.libadblockplus.android.settings.AdblockHelper
import timber.log.Timber
import java.io.File
import java.lang.ref.WeakReference
import java.net.URL
import java.util.concurrent.TimeUnit

@Suppress("DEPRECATION")
class Kaleidoscope : Application() {
    private val instanceStatePool by lazy {
        HashMap<Int, Any>()
    }
    private val frescoMemoryRegistry by lazy {
        arrayListOf<MemoryTrimmable>()
    }

    companion object {
        private var currentActivity: WeakReference<Activity>? = null

        fun getCurrentActivity(): Activity? = currentActivity?.get()

        fun saveInstanceState(any: Any): Int {
            val pool = (Boilerplate.getInstance() as Kaleidoscope).instanceStatePool

            Completable
                    .fromCallable {
                        synchronized(pool) {
                            pool.filterValues {
                                (it as WeakReference<*>).get() == null
                            }.keys.forEach {
                                pool.remove(it)
                            }
                        }
                        //keep reference for one second, works?
                        any
                    }
                    .delay(1, TimeUnit.SECONDS)
                    .subscribeOn(Schedulers.computation())
                    .subscribe()

            synchronized(pool) {
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

        RxBus.getInstance()
                .register(ActivityLifecycleEvent::class.java, ActivityLifecycleEvent::class.java)
                .subscribeBy(onNext = {
                    if (it.type == ActivityLifecycleEvent.onActivityResumed) {
                        currentActivity = WeakReference(it.activity)
                    }
                }, onError = Timber::e)

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
                                .setImagePipelineConfig(
                                        ImagePipelineConfig
                                                .newBuilder(this)
                                                .setMemoryTrimmableRegistry(object : MemoryTrimmableRegistry {
                                                    override fun registerMemoryTrimmable(trimmable: MemoryTrimmable) {
                                                        frescoMemoryRegistry.add(trimmable)
                                                    }

                                                    override fun unregisterMemoryTrimmable(trimmable: MemoryTrimmable) {
                                                        frescoMemoryRegistry.remove(trimmable)
                                                    }
                                                })
                                                .build()
                                )
                                .build()
                )) {
            logger.isDebug = Boilerplate.DEBUG

            if (Boilerplate.DEBUG) {
                StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build())
                StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder().detectAll().penaltyLog().build())
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                WebView.setWebContentsDebuggingEnabled(Boilerplate.DEBUG)
            }

            Completable
                    .fromRunnable {
                        val okHttpClientBuilder = OkHttpClient.Builder()
                        okHttpClientBuilder.apply {
                            enhance()
                            addInterceptor(PluginInterceptor.interceptor)
                            addNetworkInterceptor(PluginInterceptor.networkInterceptor)
                        }
                        URL.setURLStreamHandlerFactory(OkUrlFactory(okHttpClientBuilder.build()))

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
                                    .addEngineCreatedListener { engine ->
                                        Timber.d("adblock engine created")
                                        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                                        val networkInfo = connectivityManager.activeNetworkInfo
                                        if (networkInfo.type == ConnectivityManager.TYPE_WIFI) {
                                            map.keys.forEach { url ->
                                                engine.filterEngine.updateFiltersAsync(url)
                                            }
                                        }
                                    }
                                    .addEngineDisposedListener {
                                        Timber.d("adblock engine disposed")
                                    }
                                    .retain(true)
                        }

                        BigImageViewer.initialize(FrescoImageLoader.with(this))

                        ShareHelper.initialize(this)

                        UpdateManager.setUrl("${BuildConfig.KALEIDO_BASE_URL}updateInfo.json", "pgyer")
                    }
                    .subscribeOn(Schedulers.io())
                    .subscribe()
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        frescoMemoryRegistry.forEach {
            it.trim(MemoryTrimType.OnSystemLowMemoryWhileAppInBackground)
        }
        Timber.d("onTrimMemory")
    }
}