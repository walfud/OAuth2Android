package com.walfud.oauth2_android

import android.app.Application
import android.content.Context
import android.text.TextUtils
import org.jetbrains.anko.AnkoLogger

/**
 * Created by walfud on 22/05/2017.
 */

val context by lazy { OAuth2Application.context }

class OAuth2Application : Application(), AnkoLogger {

    companion object {
        val TAG = OAuth2Application::class.java.simpleName
        lateinit var context: Context
    }

    override fun onCreate() {
        super.onCreate()
        context = applicationContext
    }
}

fun isLogin() : Boolean {
    return !TextUtils.isEmpty(preference.oid)
}