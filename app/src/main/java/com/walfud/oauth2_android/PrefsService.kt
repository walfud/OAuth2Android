package com.walfud.oauth2_android

import org.jetbrains.anko.defaultSharedPreferences

/**
 * Created by walfud on 25/05/2017.
 */

val prefs = OAuth2Application.context!!.defaultSharedPreferences

fun setToken(token: String?) {
    prefs.edit().putString("PREFS_TOKEN", token).commit()
}
fun getToken(): String {
    return prefs.getString("PREFS_TOKEN", "")
}