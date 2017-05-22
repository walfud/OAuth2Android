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
import org.jetbrains.anko.sdk25.coroutines.onClick
import java.io.Serializable


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

        return Gson().fromJson(loginResponse.body().string(), LoginResponseBean::class.java)
    }

    fun user(token: String): UserResponseBean {
        val userResponse = OAuth2Application.okHttpClient.newCall(Request.Builder()
                .url(HttpUrl.Builder()
                        .scheme("http")
                        .host("oauth2.walfud.com")
                        .addPathSegment("user")
                        .addQueryParameter("token", token)
                        .build())
                .get()
                .build())
                .execute()
        if (!userResponse.isSuccessful) throw RuntimeException("fetch user fail")

        return Gson().fromJson(userResponse.body().string(), UserResponseBean::class.java)
    }
}

class MainActivityUI : AnkoComponent<MainActivity> {
    override fun createView(ui: AnkoContext<MainActivity>) = with(ui) {
        verticalLayout {
            val username = editText()
            val password = editText()
            button(R.string.main_login) {
                onClick {
//                    with(ui.owner) {
//                        val userResponseBean = async(CommonPool) {
//                            val loginResponseBean = login(username.text.toString(), password.text.toString())
//                            user(loginResponseBean.token)
//                        }
//                        toast(userResponseBean.await().oid)
//                    }
                    OAuth2Activity.startActivityForResult(ui.owner, "")
                }
            }
        }
    }
}

data class LoginRequestBean(val username: String, val password: String)
data class LoginResponseBean(val token: String, val refresh_token: String)
data class UserResponseBean(val oid: String) : Serializable