package com.github.gnastnosaj.filter.kaleidoscope.net

import com.github.gnastnosaj.filter.kaleidoscope.api.model.Plugin
import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber
import java.net.InetAddress
import java.util.regex.Pattern

object PluginInterceptor : Interceptor {
    class Processor {
        var type: String? = null
        var regexp: String? = null
        var args: Map<String, Any>? = null
    }

    private val processors = mutableMapOf<Plugin, Processor>()

    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        processors.entries.firstOrNull {
            val processor = it.value
            processor.type == "header" && processor.regexp != null && Pattern.compile(processor.regexp).matcher(request.url().toString()).find()
        }?.value?.let {
            (it.args?.get("headers") as? Map<String, String>)?.let { headers ->
                request = request.newBuilder().headers(Headers.of(headers)).build()
            }
        }
        var response = chain.proceed(request)
        if (!response.isSuccessful) {
            processors.entries.firstOrNull {
                val processor = it.value
                processor.type == "ip" && processor.regexp != null && Pattern.compile(processor.regexp).matcher(request.url().toString()).find()
            }?.value?.let {
                try {
                    val host = request.url().host()
                    val ip = InetAddress.getByName(host).toString().split("/")[1]
                    var url = request.url().toString()
                    Timber.d("replace $host of $url with $ip")
                    url = url.replace(host, ip)
                    request = request.newBuilder().url(url).build()
                    response = chain.proceed(request)
                } catch (throwable: Throwable) {
                    Timber.e(throwable)
                }
            }
        }
        return response
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
                processors[plugin] = processor
            }
        }
    }
}