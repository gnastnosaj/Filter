package com.github.gnastnosaj.filter.kaleidoscope.net

import com.github.gnastnosaj.filter.kaleidoscope.api.model.Plugin
import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber
import java.net.InetAddress
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

object PluginInterceptor : Interceptor {
    class Processor {
        var type: String? = null
        var regexp: String? = null
        var args: Map<String, Any>? = null
    }

    private val processors = mutableListOf<Processor>()

    override fun intercept(chain: Interceptor.Chain): Response {
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
                chain.withConnectTimeout(5, TimeUnit.SECONDS).proceed(request)
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
                    response = chain.withConnectTimeout(5, TimeUnit.SECONDS).proceed(request)
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
                    while ((response == null || response?.isSuccessful == false) && iterator.hasNext()) {
                        try {
                            var url = request.url().toString()
                            val reserve = iterator.next()
                            Timber.d("replace $origin of $url with $reserve")
                            url = url.replace(if (option.isNullOrBlank()) Regex(origin) else Regex(origin, RegexOption.valueOf(option!!)), reserve)
                            request = request.newBuilder().url(url).build()
                            response = chain.withConnectTimeout(5, TimeUnit.SECONDS).proceed(request)
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
            return it
        }
        snapshot?.let {
            throw it
        }
        throw IllegalStateException()
    }

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