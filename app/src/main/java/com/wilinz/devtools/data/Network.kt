@file:OptIn(ExperimentalEncodingApi::class)

package com.wilinz.devtools.data

import com.aallam.openai.api.logging.LogLevel
import com.aallam.openai.client.LoggingConfig
import com.aallam.openai.client.OpenAI
import com.aallam.openai.client.OpenAIHost
import com.wilinz.devtools.BuildConfig
import com.wilinz.devtools.data.api.YanxiApi
import io.ktor.client.plugins.HttpSend
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

object Network {

    val baseUrl = "https://api.nextapi.fun"

    val yanxiUrl = "https://tk.enncy.cn/"

    val baseOkhttpClientBuilder
        get() = OkHttpClient.Builder().apply {
//            cookieJar(cookieJar)
            this.callTimeout(2, TimeUnit.MINUTES)
            this.connectTimeout(2, TimeUnit.MINUTES)
            this.readTimeout(2, TimeUnit.MINUTES)
            this.writeTimeout(2, TimeUnit.MINUTES)
//            addInterceptor {
//                val req = it.request().newBuilder()
//                req.addHeader("app-id", BuildConfig.APPLICATION_ID)
//                req.addHeader("app-version-code", BuildConfig.VERSION_CODE.toString())
//                req.addHeader("app-version-name", BuildConfig.VERSION_NAME)
//                it.proceed(req.build())
//            }
        }

    val yanxiRetrofit = Retrofit.Builder()
        .client(baseOkhttpClientBuilder.addLogger().build())
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .baseUrl(yanxiUrl)
        .build()

    val yanxiApi = yanxiRetrofit.create(YanxiApi::class.java)

    private fun OkHttpClient.Builder.addLogger() = this.addInterceptor(HttpLoggingInterceptor().apply {
        level =
            if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.BODY
    })

    val key = Base64.decode("YWstc2VKcHB5VnhZSklpQlFya1JFYng0dGlFNWFTaHFKSGFVVTBvMVRVeFd6azdJdm5r").toString(Charsets.UTF_8)

    val openAI = OpenAI(
        token = key,
        logging = LoggingConfig(LogLevel.None),
        host = OpenAIHost(baseUrl = baseUrl),
//        onClientCreated = {
//            it.plugin(HttpSend).intercept { request ->
//                fun cookieHeader(cookies: List<Cookie>): String = buildString {
//                    cookies.forEachIndexed { index, cookie ->
//                        if (index > 0) append("; ")
//                        append(cookie.name).append('=').append(cookie.value)
//                    }
//                }
//
//                val cookie = cookieJar.loadForRequest(request.url.buildString().toHttpUrl())
//                request.headers.append("Cookie", cookieHeader(cookie))
//                val call = execute(request)
//                if (call.response.status.value == 401) {
//                    kotlin.runCatching { UserRepository.logoutLocal() }
//                    toast("请登录")
//                }
//                call
//            }
//        },
    )

}