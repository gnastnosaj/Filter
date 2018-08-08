package com.github.gnastnosaj.filter.magneto

import android.content.Context
import android.text.TextUtils
import com.github.gnastnosaj.filter.dsl.groovy.GrooidClassLoader
import com.github.gnastnosaj.filter.magneto.util.NetworkUtil
import groovy.lang.Script
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.HttpLoggingInterceptor.Level
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.lang.reflect.Type
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager


class MagnetoRetrofit(private val context: Context) {
    val service: MagnetoService

    init {
        val logging = HttpLoggingInterceptor()
        logging.level = Level.BASIC

        val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .hostnameVerifier { _, _ -> true }
                .sslSocketFactory(createSSLSocketFactory(), TrustAllCerts())
                .addNetworkInterceptor(logging)
                .cache(Cache(File(context.externalCacheDir, "okHttpCache"), 100 * 1024 * 1024))
                .addNetworkInterceptor { chain ->
                    var response = chain.proceed(chain.request())
                    val cacheControl = response.cacheControl().toString()
                    if (NetworkUtil.isAvailable(context) && (cacheControl.contains("no-store") || cacheControl.contains("must-revalidate") || cacheControl.contains("no-cache") || cacheControl.contains("max-age=0"))) {
                        response = response.newBuilder()
                                .removeHeader("Pragma")
                                .header("Cache-Control", "private, max-age=$MAX_AGE")
                                .build()
                    }
                    response
                }
                .addInterceptor { chain ->
                    var request = chain.request()
                    if (!NetworkUtil.isAvailable(context) && TextUtils.isEmpty(request.cacheControl().toString())) {
                        request = request.newBuilder()
                                .removeHeader("Pragma")
                                .header("Cache-Control", "private, only-if-cached, max-stale=$MAX_STALE")
                                .build()
                    }
                    chain.proceed(request)
                }
                .build()

        val retrofit = Retrofit.Builder()
                .client(okHttpClient)
                .baseUrl(context.resources.getString(R.string.magneto_base_url))
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(object : Converter.Factory() {
                    override fun responseBodyConverter(type: Type?, annotations: Array<out Annotation>?, retrofit: Retrofit?): Converter<ResponseBody, *>? {
                        return if (type == Script::class.java) {
                            Converter<ResponseBody, Script> {
                                GrooidClassLoader.loadAndCreateGroovyObject(context, it.string()) as Script
                            }
                        } else {
                            null
                        }
                    }
                })
                .addConverterFactory(GsonConverterFactory.create())
                .build()

        service = retrofit.create(MagnetoService::class.java)
    }

    class TrustAllCerts : X509TrustManager {
        @Throws(CertificateException::class)
        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {

        }

        @Throws(CertificateException::class)
        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {

        }

        override fun getAcceptedIssuers(): Array<X509Certificate>? {
            return arrayOf()
        }
    }

    companion object {
        private const val MAX_STALE = 30
        private const val MAX_AGE = 60 * 60

        fun createSSLSocketFactory(): SSLSocketFactory {
            val sc = SSLContext.getInstance("TLS")
            sc.init(null, arrayOf<TrustManager>(TrustAllCerts()), SecureRandom())
            return sc.socketFactory
        }
    }
}