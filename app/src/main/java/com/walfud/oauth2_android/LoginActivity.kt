package com.walfud.oauth2_android

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.google.gson.Gson
import com.walfud.walle.algorithm.hash.HashUtils
import okhttp3.HttpUrl
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.jetbrains.anko.*
import org.jetbrains.anko.coroutines.experimental.bg
import org.jetbrains.anko.sdk25.coroutines.onClick


class LoginActivity : BaseActivity() {

    companion object {
        val EXTRA_OID = "EXTRA_OID"

        fun startActivityForResult(activity: Activity, requestId: Int) {
            val intent = Intent(activity, LoginActivity::class.java)
            activity.startActivityForResult(intent, requestId)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LoginActivityUI().setContentView(this)
    }

    fun login(username: String, password: String): LoginResponseBean {
        val loginRequestBean = LoginRequestBean(username,
                HashUtils.md5(password))
        val loginResponse = OAuth2Application.okHttpClient.newCall(Request.Builder()
                .url("http://oauth2.walfud.com/login")
                .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"),
                        Gson().toJson(loginRequestBean)))
                .build())
                .execute()
        if (!loginResponse.isSuccessful) throw RuntimeException("login fail")

        return getResponse(loginResponse.body().string(), LoginResponseBean::class.java)
    }

    fun user(): UserResponseBean {
        val userResponse = OAuth2Application.okHttpClient.newCall(Request.Builder()
                .url(HttpUrl.Builder()
                        .scheme("http")
                        .host("oauth2.walfud.com")
                        .addPathSegment("user")
                        .build())
                .get()
                .build())
                .execute()
        if (!userResponse.isSuccessful) throw RuntimeException("fetch user fail")

        return getResponse(userResponse.body().string(), UserResponseBean::class.java)
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
                            setToken(loginResponseBean.accessToken)

                            val userResponse = bg { user() }
                            val userResponseBean = userResponse.await()

                            finish(null, bundleOf(LoginActivity.EXTRA_OID to userResponseBean.oid))
                        } catch (e: Exception) {
                            finish(e.message, null)
                        }
                    }
                }
            }
        }
    }
}