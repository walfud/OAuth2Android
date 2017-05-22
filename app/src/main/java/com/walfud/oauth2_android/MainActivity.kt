package com.walfud.oauth2_android

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.google.gson.Gson
import com.walfud.walle.algorithm.hash.HashUtils
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import okhttp3.*
import org.jetbrains.anko.*
import org.jetbrains.anko.sdk25.coroutines.onClick
import java.io.Serializable


class MainActivity : Activity(), AnkoLogger {

    companion object {
        val EXTRA_TOKEN: String = "EXTRA_TOKEN"

        val EXTRA_OID: String = "EXTRA_OID"
    }

    val mOkHttpClient: OkHttpClient = OkHttpClient.Builder()
            .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!intent.hasExtra(EXTRA_TOKEN)) {
            MainActivityUI().setContentView(this)
        } else {
            MainActivityTokenUI().setContentView(this)
        }
    }

    fun login(username: String, password: String): LoginResponseBean {
        val loginRequestBean = LoginRequestBean(username,
                HashUtils.md5(password))
        val loginResponse = mOkHttpClient.newCall(Request.Builder()
                .url("http://oauth2.walfud.com/login")
                .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"),
                        Gson().toJson(loginRequestBean)))
                .build())
                .execute()
        if (!loginResponse.isSuccessful) throw RuntimeException("login fail")

        return Gson().fromJson(loginResponse.body().string(), LoginResponseBean::class.java)
    }

    fun user(token: String): UserResponseBean {
        val userResponse = mOkHttpClient.newCall(Request.Builder()
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

    fun finish(oid: String) {
        val intent = Intent()
        intent.putExtra(EXTRA_OID, oid)
        setResult(RESULT_OK, intent)
        finish()
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
                        val userResponseBean = async(CommonPool) {
                            val loginResponseBean = login(username.text.toString(), password.text.toString())
                            user(loginResponseBean.token)
                        }
                        toast(userResponseBean.await().oid)
                    }
                }
            }
        }
    }
}

class MainActivityTokenUI : AnkoComponent<MainActivity> {
    override fun createView(ui: AnkoContext<MainActivity>) = with(ui) {
        verticalLayout {
            button(R.string.main_authorize) {
                onClick {
                    with(ui.owner) {
                        val userResponseBean = async(CommonPool) {
                            user(intent.getStringExtra(MainActivity.EXTRA_TOKEN))
                        }
                        finish(userResponseBean.await().oid)
                    }
                }
            }
        }
    }
}

data class LoginRequestBean(val username: String, val password: String)
data class LoginResponseBean(val token: String, val refresh_token: String)
data class UserResponseBean(val oid: String) : Serializable