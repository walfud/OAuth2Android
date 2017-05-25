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
                .build()
        var context: Context? = null
    }

    override fun onCreate() {
        super.onCreate()
        context = applicationContext
    }
}