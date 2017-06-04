package com.walfud.oauth2_android

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import org.jetbrains.anko.*
import org.jetbrains.anko.sdk25.coroutines.onClick

/**
 * Created by walfud on 30/05/2017.
 */

val REQUEST_TEST_LOGIN = 8
val REQUEST_TEST_OAUTH2 = 9

class MainActivity : BaseActivity() {

    lateinit var usernameTv: TextView
    lateinit var appNameTv: TextView
    lateinit var oidTv: TextView
    lateinit var accessTokenTv: TextView
    lateinit var refreshTokenTv: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MainActivityUI().setContentView(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        data!!
        if (resultCode == Activity.RESULT_OK) {
            usernameTv.text = data.getStringExtra(EXTRA_USERNAME)
            appNameTv.text = data.getStringExtra(EXTRA_APPNAME)
            oidTv.text = data.getStringExtra(EXTRA_OID)
            accessTokenTv.text = data.getStringExtra(EXTRA_ACCESS_TOKEN)
            refreshTokenTv.text = data.getStringExtra(EXTRA_REFRESH_TOKEN)
        } else {
            toast(data.getStringExtra(EXTRA_ERROR))
        }
    }
}

class MainActivityUI : AnkoComponent<MainActivity> {
    override fun createView(ui: AnkoContext<MainActivity>) = with(ui) {
        verticalLayout {
            button("Test Login") {
                onClick {
                    startOAuth2ActivityForResult(owner, REQUEST_TEST_LOGIN)
                }
            }
            button("Test Logout") {
                onClick {
                    preference.oid = null
                }
            }
            button("Test OAuth2") {
                onClick {
                    startOAuth2ActivityForResult(owner, REQUEST_TEST_OAUTH2, "contactsync")
                }
            }

            owner.usernameTv = textView()
            owner.appNameTv = textView()
            owner.oidTv = textView()
            owner.accessTokenTv = textView()
            owner.refreshTokenTv = textView()
        }
    }
}