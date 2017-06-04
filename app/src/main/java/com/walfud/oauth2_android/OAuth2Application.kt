package com.walfud.oauth2_android

import android.app.Application
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Transformations
import android.content.Context
import android.text.TextUtils
import org.jetbrains.anko.AnkoLogger

/**
 * Created by walfud on 22/05/2017.
 */

val context by lazy { OAuth2Application.context }
val oidLiveData = MutableLiveData<String>()
val oauth2LiveData = Transformations.switchMap(oidLiveData, { oid -> database.oauth2Dao().query(oid) })
val userLiveData = Transformations.switchMap(oauth2LiveData, { (user_name) -> database.userDao().query(user_name!!) })
val appLiveData = Transformations.switchMap(oauth2LiveData, { (_, app_name) -> database.appDao().query(app_name!!) })

class OAuth2Application : Application(), AnkoLogger {

    companion object {
        val TAG = OAuth2Application::class.java.simpleName
        lateinit var context: Context
    }

    override fun onCreate() {
        super.onCreate()
        context = applicationContext

        oidLiveData.value = preference.oid
    }
}

fun isLogin() : Boolean {
    return !TextUtils.isEmpty(preference.oid)
}