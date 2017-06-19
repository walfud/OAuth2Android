package com.walfud.oauth2_android

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import com.walfud.oauth2_android.dagger2.DaggerMainComponent
import com.walfud.oauth2_android.dagger2.MainModule
import org.jetbrains.anko.*
import org.jetbrains.anko.sdk25.coroutines.onClick
import javax.inject.Inject

/**
 * Created by walfud on 30/05/2017.
 */

val REQUEST_TEST_LOGIN = 8
val REQUEST_TEST_TOKEN = 9
val REQUEST_TEST_OAUTH2 = 10

class MainActivity : BaseActivity() {

    lateinit var usernameTv: TextView
    lateinit var appNameTv: TextView
    lateinit var oidTv: TextView
    lateinit var accessTokenTv: TextView
    lateinit var refreshTokenTv: TextView

    var oid: String? = null
    var accessToken: String? = null

    @Inject lateinit var preference: Preference
    @Inject lateinit var database: Database
    @Inject lateinit var network: Network

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MainActivityUI().setContentView(this)

        DaggerMainComponent.builder()
                .applicationComponent((application as OAuth2Application).component)
                .mainModule(MainModule(this))
                .build()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        data!!
        if (resultCode != Activity.RESULT_OK) {
            toast(data.getStringExtra(EXTRA_ERROR))
            return
        }

        when (requestCode) {
            REQUEST_TEST_LOGIN -> {
                val loginResponseBean = data.getSerializableExtra(EXTRA_LOGIN_RESPONSE_BEAN) as LoginResponseBean
                accessToken = loginResponseBean.accessToken
            }
            REQUEST_TEST_OAUTH2 -> {
                usernameTv.text = data.getStringExtra(EXTRA_USERNAME)
                appNameTv.text = data.getStringExtra(EXTRA_APPNAME)
                oidTv.text = data.getStringExtra(EXTRA_OID)
                accessTokenTv.text = data.getStringExtra(EXTRA_ACCESS_TOKEN)
                refreshTokenTv.text = data.getStringExtra(EXTRA_REFRESH_TOKEN)
            }
        }
    }
}

class MainActivityUI : AnkoComponent<MainActivity> {
    override fun createView(ui: AnkoContext<MainActivity>) = with(ui) {
        verticalLayout {
            button("Test Logout") {
                onClick {
                    Preference(owner).oid = null
                }
            }
            button("Test Login") {
                onClick {
                    LoginActivity.startActivityForResult(owner, REQUEST_TEST_LOGIN)
                }
            }
            button("Test Token") {
                onClick {
                    TokenActivity.startActivityForResult(owner, REQUEST_TEST_TOKEN, owner.accessToken!!, "contactsync")
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