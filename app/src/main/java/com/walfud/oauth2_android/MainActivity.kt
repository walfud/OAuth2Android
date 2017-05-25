package com.walfud.oauth2_android

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import org.jetbrains.anko.*
import org.jetbrains.anko.sdk25.coroutines.onClick

/**
 * Created by walfud on 25/05/2017.
 */

class MainActivity : BaseActivity() {
    companion object {
        val EXTRA_CLIENT_ID = OAuth2Activity.EXTRA_CLIENT_ID
        val REQUEST_LOGIN = 1
        val REQUEST_OAUTH2 = 2
        val REQUEST_TEST = 9

        val EXTRA_ACCESS_TOKEN = "EXTRA_ACCESS_TOKEN"
        val EXTRA_REFRESH_TOKEN = "EXTRA_REFRESH_TOKEN"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val clientId = intent.getStringExtra(EXTRA_CLIENT_ID)
        if (TextUtils.isEmpty(clientId)) {
            MainActivityUI().setContentView(this)
        } else {
            // OAuth2
            if (TextUtils.isEmpty(getToken())) {
                // Anonymous
                LoginActivity.startActivityForResult(this, REQUEST_LOGIN)
            } else {
                // Login
                OAuth2Activity.startActivityForResult(this, REQUEST_OAUTH2, clientId)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_LOGIN -> {
                if (resultCode == RESULT_OK) {
                    OAuth2Activity.startActivityForResult(this, REQUEST_OAUTH2, intent.getStringExtra(OAuth2Activity.EXTRA_CLIENT_ID))
                } else {
                    finish(if (data != null) data.getStringExtra(EXTRA_ERROR) else resources.getString(R.string.main_user_cancel), null)
                }
            }
            REQUEST_OAUTH2 -> {
                if (resultCode == RESULT_OK) {
                    val tokenResponseBean = data!!.getSerializableExtra(OAuth2Activity.EXTRA_TOKEN_RESPONSE_BEAN) as TokenResponseBean
                    finish(null, bundleOf(EXTRA_ACCESS_TOKEN to tokenResponseBean.accessToken,
                            EXTRA_REFRESH_TOKEN to tokenResponseBean.refreshToken))
                } else {
                    finish(if (data != null) data.getStringExtra(EXTRA_ERROR) else resources.getString(R.string.main_user_cancel), null)
                }
            }
            REQUEST_TEST -> {
                if (resultCode == RESULT_OK) {
                    val accessToken = data!!.getStringExtra(EXTRA_ACCESS_TOKEN)
                    val refreshToken = data!!.getStringExtra(EXTRA_REFRESH_TOKEN)
                    toast("accessToken($accessToken) / refreshToken($refreshToken})")
                } else {
                    toast(if (data != null) data.getStringExtra(EXTRA_ERROR) else resources.getString(R.string.main_user_cancel))
                }
            }
        }
    }
}

class MainActivityUI : AnkoComponent<MainActivity> {
    override fun createView(ui: AnkoContext<MainActivity>) = with(ui) {
        verticalLayout {
            button("Login Activity") {
                onClick {
                    with(ui.owner) {
                        startActivity<LoginActivity>()
                    }
                }
            }
            button("OAuth2 Activity") {
                onClick {
                    with(ui.owner) {
                        startActivity<OAuth2Activity>(OAuth2Activity.EXTRA_CLIENT_ID to "contactsync")
                    }
                }
            }
            button("Test") {
                onClick {
                    with(ui.owner) {
                        val intent = Intent(this, MainActivity::class.java)
                        intent.putExtra(MainActivity.EXTRA_CLIENT_ID, "contactsync")
                        startActivityForResult(intent, MainActivity.REQUEST_TEST)
                    }
                }
            }
        }
    }
}