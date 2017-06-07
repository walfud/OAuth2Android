package com.walfud.oauth2_android

import android.content.Context
import org.jetbrains.anko.defaultSharedPreferences

/**
 * Created by walfud on 25/05/2017.
 */

const private val PREFS_OID = "PREFS_OID"

class Preference(context: Context) {
    val prefs = context.defaultSharedPreferences

    var oid: String?
        get() {
            return prefs.getString(PREFS_OID, null)
        }
        set(value) {
            prefs.edit().putString(PREFS_OID, value).commit()
        }
}
