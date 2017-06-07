package com.walfud.oauth2_android

import android.app.Application
import com.walfud.oauth2_android.dagger2.ApplicationComponent
import com.walfud.oauth2_android.dagger2.ApplicationModule
import com.walfud.oauth2_android.dagger2.DaggerApplicationComponent
import org.jetbrains.anko.AnkoLogger

/**
 * Created by walfud on 22/05/2017.
 */

class OAuth2Application : Application(), AnkoLogger {

    companion object {
        val TAG = OAuth2Application::class.java.simpleName
    }

    val component: ApplicationComponent by lazy {
        DaggerApplicationComponent.builder()
                .applicationModule(ApplicationModule(this))
                .build()
    }

    override fun onCreate() {
        super.onCreate()
        component.inject(this)
    }
}