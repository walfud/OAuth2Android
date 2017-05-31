package com.walfud.oauth2_android

import org.jetbrains.anko.defaultSharedPreferences

/**
 * Created by walfud on 25/05/2017.
 */

val preference by lazy { Preference() }

const private val PREFS_OID = "PREFS_OID"

class Preference {
    val prefs = OAuth2Application.context.defaultSharedPreferences

    var oid: String?
        get() {
            return prefs.getString(PREFS_OID, null)
        }
        set(value) {
            prefs.edit().putString(PREFS_OID, value).commit()
        }
}
