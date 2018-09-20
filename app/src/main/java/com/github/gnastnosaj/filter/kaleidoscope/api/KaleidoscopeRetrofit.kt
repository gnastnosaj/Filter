package com.github.gnastnosaj.filter.kaleidoscope.api

import android.content.Context
import com.github.gnastnosaj.boilerplate.Boilerplate
import com.github.gnastnosaj.filter.dsl.groovy.GrooidClassLoader
import com.github.gnastnosaj.filter.kaleidoscope.BuildConfig
import com.github.gnastnosaj.filter.kaleidoscope.net.OkHttpEnhancer.enhance
import groovy.lang.Script
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.lang.reflect.Type
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager


class KaleidoscopeRetrofit(private val context: Context) {
    val service: KaleidoscopeService
    val okHttpClient: OkHttpClient

    init {
        val okHttpClientBuilder = OkHttpClient.Builder()
        okHttpClientBuilder.apply {
            connectTimeout(30, TimeUnit.SECONDS)
            retryOnConnectionFailure(true)
            hostnameVerifier { _, _ -> true }
            sslSocketFactory(createSSLSocketFactory(), TrustAllCerts())
            enhance()
        }

        okHttpClient = okHttpClientBuilder.build()

        val retrofit = Retrofit.Builder()
                .client(okHttpClient)
                .baseUrl(BuildConfig.KALEIDO_BASE_URL)
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

        service = retrofit.create(KaleidoscopeService::class.java)
    }

    class TrustAllCerts : X509TrustManager {
        @Throws(CertificateException::class)
        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {

        }

        @Throws(CertificateException::class)
        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {

        }

        override fun getAcceptedIssuers(): Array<X509Certificate> {
            return arrayOf()
        }
    }

    companion object {
        val instance by lazy {
            KaleidoscopeRetrofit(Boilerplate.getInstance())
        }

        fun createSSLSocketFactory(): SSLSocketFactory {
            val sc = SSLContext.getInstance("TLS")
            sc.init(null, arrayOf<TrustManager>(TrustAllCerts()), SecureRandom())
            return sc.socketFactory
        }
    }
}