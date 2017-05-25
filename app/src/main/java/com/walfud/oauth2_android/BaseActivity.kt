package com.walfud.oauth2_android

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import org.jetbrains.anko.AnkoLogger

/**
 * Created by walfud on 25/05/2017.
 */

open class BaseActivity : Activity(), AnkoLogger {
    companion object {
        val EXTRA_ERROR: String = "EXTRA_ERROR"
    }

    fun finish(err: String?, bundle: Bundle?) {
        val intent = Intent()
        if (!TextUtils.isEmpty(err)) {
            intent.putExtra(EXTRA_ERROR, err)
            setResult(RESULT_CANCELED, intent)
        } else {
            bundle?.let { intent.putExtras(bundle) }
            setResult(RESULT_OK, intent)
        }
        finish()
    }
}