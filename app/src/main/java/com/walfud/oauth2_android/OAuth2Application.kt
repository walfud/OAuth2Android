package com.walfud.oauth2_android

import android.app.Application

/**
 * Created by walfud on 2017/7/25.
 */

class OAuth2Application : Application() {
    companion object {
        lateinit var component: ApplicationComponent
    }

    override fun onCreate() {
        super.onCreate()

        component = DaggerApplicationComponent.builder()
                .applicationModule(ApplicationModule(this))
                .build()

        try {
            Thread.sleep(2000)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}