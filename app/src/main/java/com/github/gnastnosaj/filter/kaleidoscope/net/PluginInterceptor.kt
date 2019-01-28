package com.github.gnastnosaj.filter.kaleidoscope.net

import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import com.github.gnastnosaj.filter.kaleidoscope.Kaleidoscope
import com.github.gnastnosaj.filter.kaleidoscope.api.model.Plugin
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import okhttp3.*
import timber.log.Timber
import java.net.HttpURLConnection.*
import java.net.InetAddress
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

object PluginInterceptor {
    val interceptor = Interceptor { chain ->
        var request = chain.request()
        processors.firstOrNull { processor ->
            processor.type == "header" && processor.regexp != null && Pattern.compile(processor.regexp).matcher(request.url().toString()).find()
        }?.let {
            (it.args?.get("headers") as? Map<String, String>)?.let { headers ->
                request = request.newBuilder().headers(Headers.of(headers)).build()
            }
        }

        var response: Response? = null
        var snapshot: Throwable? = null
        try {
            response = if (processors.none { processor ->
                        processor.regexp != null && Pattern.compile(processor.regexp).matcher(request.url().toString()).find()
                    }) {
                chain.proceed(request)
            } else {
                chain.withConnectTimeout(6, TimeUnit.SECONDS).proceed(request)
            }
        } catch (throwable: Throwable) {
            snapshot = throwable
        }

        if (response == null || !response.isSuccessful) {
            processors.firstOrNull { processor ->
                processor.type == "ip" && processor.regexp != null && Pattern.compile(processor.regexp).matcher(request.url().toString()).find()
            }?.let {
                try {
                    val host = request.url().host()
                    val ip = InetAddress.getByName(host).toString().split("/")[1]
                    var url = request.url().toString()
                    Timber.d("replace $host of $url with $ip")
                    url = url.replace(host, ip)
                    request = request.newBuilder().url(url).build()
                    response = chain.withConnectTimeout(15, TimeUnit.SECONDS).proceed(request)
                } catch (throwable: Throwable) {
                    snapshot = throwable
                }
            }

            processors.firstOrNull { processor ->
                processor.type == "reserve" && processor.regexp != null && Pattern.compile(processor.regexp).matcher(request.url().toString()).find()
            }?.let { processor ->
                try {
                    val origin = processor.args?.get("origin") as String
                    val option = processor.args?.get("option") as? String
                    val reserves = processor.args?.get("reserves") as List<String>
                    val iterator = reserves.iterator()
                    val url = request.url().toString()
                    while ((response == null || response?.isSuccessful == false) && iterator.hasNext()) {
                        try {
                            val reserve = iterator.next()
                            Timber.d("replace $origin of $url with $reserve")
                            request = request.newBuilder().url(url.replace(if (option.isNullOrBlank()) Regex(origin) else Regex(origin, RegexOption.valueOf(option!!)), reserve)).build()
                            response = chain.withConnectTimeout(15, TimeUnit.SECONDS).proceed(request)
                        } catch (throwable: Throwable) {
                            snapshot = throwable
                        }
                    }
                } catch (throwable: Throwable) {
                    Timber.e(throwable)
                }
            }
        }

        response?.let {
            if (it.code() == HTTP_UNAVAILABLE) {
                it.header("Location")?.let { location ->
                    Timber.d("start cloudflare bypass, request url: $location")
                    val httpUrl = request.url()
                    val host = request.url().host()
                    val referer = request.url().toString()
                    request = request.newBuilder().apply {
                        url(httpUrl.newBuilder(location).toString())
                        header("HOST", host)
                        header("Referer", referer)
                        it.header("Set-Cookie")?.let {
                            header("Cookie", it)
                        }
                    }.build()
                    response = chain.withConnectTimeout(15, TimeUnit.SECONDS).proceed(request)
                }
            }
        }

        response?.let {
            return@Interceptor it
        }
        snapshot?.let {
            throw it
        }
        throw IllegalStateException()
    }

    val networkInterceptor = Interceptor { chain ->
        var request = chain.request()
        var response = chain.proceed(request)

        response?.let {
            if (it.code() == HTTP_UNAVAILABLE) {
                val body = it.body()?.string()
                processors.firstOrNull { processor ->
                    processor.type == "cloudflare" && processor.regexp != null && body != null && Pattern.compile(processor.regexp).matcher(body).find()
                }?.let {
                    val url = request.url().toString()
                    val countDownLatch = CountDownLatch(1)

                    Completable
                            .fromRunnable {
                                Kaleidoscope.getCurrentActivity()?.let { activity ->
                                    val cookieManager = CookieManager.getInstance()

                                    WebView(activity).apply {
                                        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                            cookieManager.setAcceptThirdPartyCookies(this, true)
                                        }

                                        request.header("User-Agent")?.let {
                                            settings.userAgentString = it
                                        }
                                        settings.javaScriptEnabled = true

                                        val timeout = Observable.timer(15, TimeUnit.SECONDS)
                                                .observeOn(AndroidSchedulers.mainThread())
                                                .subscribeBy(onNext = {
                                                    activity.findViewById<ViewGroup>(android.R.id.content).removeView(this)
                                                    destroy()
                                                }, onError = Timber::e)

                                        val check: (String) -> Boolean = { newUrl ->
                                            val httpUrl = HttpUrl.get(url)
                                            val newHttpUrl = HttpUrl.get(newUrl)
                                            if (newHttpUrl == httpUrl) {
                                                val setCookie = cookieManager.getCookie(newUrl)
                                                val cookie = Cookie.parse(httpUrl, setCookie)
                                                if (cookie != null && cookie.expiresAt() > System.currentTimeMillis() && setCookie.contains("cf_clearance")) {
                                                    response = response.newBuilder().header("Location", newUrl).header("Set-Cookie", setCookie).build()
                                                    OkHttpEnhancer.cookieJar.saveFromResponse(newHttpUrl, mutableListOf(Cookie.parse(newHttpUrl, setCookie)))
                                                    countDownLatch.countDown()
                                                    timeout.dispose()
                                                    activity.findViewById<ViewGroup>(android.R.id.content).removeView(this@apply)
                                                    destroy()
                                                    true
                                                } else {
                                                    false
                                                }
                                            } else {
                                                false
                                            }
                                        }

                                        webViewClient = object : WebViewClient() {
                                            override fun onPageFinished(view: WebView, newUrl: String) {
                                                check(newUrl)
                                            }

                                            override fun shouldOverrideUrlLoading(view: WebView, newUrl: String): Boolean {
                                                return check(newUrl)
                                            }
                                        }

                                        visibility = View.GONE
                                        activity.findViewById<ViewGroup>(android.R.id.content).addView(this@apply)
                                        Timber.d("start cloudflare bypass, webview load url: $url")
                                        loadUrl(url)
                                    }
                                }
                            }
                            .subscribeOn(AndroidSchedulers.mainThread())
                            .subscribeBy(onError = Timber::e)
                    countDownLatch.await(30, TimeUnit.SECONDS)
                }
            }
        }

        response?.let {
            return@Interceptor it
        }
        throw IllegalStateException()
    }

    class Processor {
        var type: String? = null
        var regexp: String? = null
        var args: Map<String, Any>? = null
    }

    private val processors = mutableListOf<Processor>()

    fun plugins(vararg plugins: Plugin) {
        plugins(plugins.toList())
    }

    fun plugins(plugins: List<Plugin>) {
        plugins.forEach { plugin ->
            (plugin.args?.get("processors") as? List<Map<String, Any>>)?.forEach {
                val processor = Processor()
                processor.type = it["type"] as? String
                processor.regexp = it["regexp"] as? String
                processor.args = it["args"] as? Map<String, Any>
                processors.add(processor)
            }
        }
    }
}