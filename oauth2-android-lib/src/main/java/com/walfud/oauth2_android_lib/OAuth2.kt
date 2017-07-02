package com.walfud.oauth2_android_lib

import android.app.Activity
import android.content.Context
import com.walfud.oauth2_android_lib.activity.startOAuth2ActivityForResult
import com.walfud.oauth2_android_lib.util.LiveDataCallAdapterFactory
import com.walfud.oauth2_android_lib.util.Network
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Created by walfud on 2017/7/2.
 */

val EXTRA_ERROR = "EXTRA_ERROR"
val EXTRA_USERNAME = "EXTRA_USERNAME"
val EXTRA_APPNAME = "EXTRA_APPNAME"
val EXTRA_OID = "EXTRA_OID"
val EXTRA_ACCESS_TOKEN = "EXTRA_ACCESS_TOKEN"
val EXTRA_REFRESH_TOKEN = "EXTRA_REFRESH_TOKEN"

class OAuth2 {
    companion object {
        var init = false
        lateinit var context: Context
        lateinit var network: Network
    }
}

fun startOAuth2ForResult(activity: Activity, requestCode: Int, appName: String) {
    if (!OAuth2.init) {
        OAuth2.context = activity.applicationContext
        OAuth2.network = Retrofit.Builder()
                .baseUrl("http://oauth2.walfud.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(LiveDataCallAdapterFactory())
                .client(OkHttpClient.Builder()
                        .addInterceptor {
                            it.proceed(it.request().newBuilder()
                                    .build())
                        }
                        .build())
                .build()
                .create(Network::class.java)

        OAuth2.init = true
    }

    startOAuth2ActivityForResult(activity, requestCode, appName)
}