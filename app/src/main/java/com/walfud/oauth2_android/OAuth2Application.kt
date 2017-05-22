package com.walfud.oauth2_android

import android.app.Application
import okhttp3.OkHttpClient
import org.jetbrains.anko.AnkoLogger

/**
 * Created by walfud on 22/05/2017.
 */

class OAuth2Application : Application(), AnkoLogger {

    companion object {
        val okHttpClient: OkHttpClient = OkHttpClient.Builder()
                .build()
    }

    override fun onCreate() {
        super.onCreate()
    }
}