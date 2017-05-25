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


class MainActivity : Activity(), AnkoLogger {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MainActivityUI().setContentView(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            OAuth2Activity.ID_REQUEST -> {
                if (resultCode == RESULT_OK) {
                    val tokenResponseBean = data?.getSerializableExtra(OAuth2Activity.EXTRA_TOKEN_RESPONSE_BEAN) as TokenResponseBean
                    toast(tokenResponseBean.accessToken)
                } else {
                    toast(data?.getStringExtra(OAuth2Activity.EXTRA_ERROR) ?: "")
                }
            }
        }
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

class MainActivityUI : AnkoComponent<MainActivity> {
    override fun createView(ui: AnkoContext<MainActivity>) = with(ui) {
        verticalLayout {
            val username = editText()
            val password = editText()
            button(R.string.main_login) {
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
                            toast(userResponseBean.oid)
                        } catch (e: Exception) {
                            toast(e.message ?: "Unknown Error")
                        }
                    }
                }
            }
            button("Test") {
                onClick {
                    with(ui.owner) {
                        OAuth2Activity.startActivityForResult(ui.owner, "contactsync")
                    }
                }
            }
        }
    }
}