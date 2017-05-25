package com.walfud.oauth2_android

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.walfud.walle.algorithm.hash.HashUtils
import org.jetbrains.anko.*
import org.jetbrains.anko.coroutines.experimental.bg
import org.jetbrains.anko.sdk25.coroutines.onClick


class LoginActivity : BaseActivity() {

    companion object {
        fun startActivityForResult(activity: Activity, requestId: Int) {
            val intent = Intent(activity, LoginActivity::class.java)
            activity.startActivityForResult(intent, requestId)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LoginActivityUI().setContentView(this)
    }
}

class LoginActivityUI : AnkoComponent<LoginActivity> {
    override fun createView(ui: AnkoContext<LoginActivity>) = with(ui) {
        verticalLayout {
            val username = editText()
            val password = editText()
            button(R.string.login_login) {
                onClick {
                    with(ui.owner) {
                        try {
                            val username = username.text.toString()
                            val password = password.text.toString()
                            val loginResponse = bg { login(username, HashUtils.md5(password)) }
                            val loginResponseBean = loginResponse.await()

                            val userResponse = bg { user(loginResponseBean.accessToken) }
                            val userResponseBean = userResponse.await()

                            setToken(loginResponseBean.accessToken)

                            finish(null, bundleOf(EXTRA_OID to userResponseBean.oid,
                                    EXTRA_ACCESS_TOKEN to loginResponseBean.accessToken,
                                    EXTRA_REFRESH_TOKEN to loginResponseBean.refreshToken))
                        } catch (e: Exception) {
                            finish(e.message, null)
                        }
                    }
                }
            }
        }
    }
}