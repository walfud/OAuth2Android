package com.walfud.oauth2_android

import android.app.Application
import android.content.Context
import okhttp3.OkHttpClient
import org.jetbrains.anko.AnkoLogger

/**
 * Created by walfud on 22/05/2017.
 */

class OAuth2Application : Application(), AnkoLogger {

    companion object {
        val okHttpClient: OkHttpClient = OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val request = chain.request()
                    if (request.url().pathSegments().isNotEmpty()
                            && request.url().pathSegments()[0].equals("login")) {
                        // Pass login api
                        chain.proceed(request)
                    } else {
                        chain.proceed(request.newBuilder().header("X-Access-Token", getToken()).build())
                    }
                }
                .build()
        var context: Context? = null
    }

    override fun onCreate() {
        super.onCreate()
        context = applicationContext
    }
}