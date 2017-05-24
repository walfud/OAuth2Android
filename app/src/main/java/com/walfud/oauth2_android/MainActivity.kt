package com.walfud.oauth2_android

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.walfud.walle.algorithm.hash.HashUtils
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import okhttp3.HttpUrl
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.jetbrains.anko.*
import org.jetbrains.anko.sdk25.coroutines.onClick
import org.json.JSONObject
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

        return getResponse(loginResponse.body().string(), LoginResponseBean::class.java)
    }

    fun user(token: String): UserResponseBean {
        val userResponse = OAuth2Application.okHttpClient.newCall(Request.Builder()
                .url(HttpUrl.Builder()
                        .scheme("http")
                        .host("oauth2.walfud.com")
                        .addPathSegment("user")
                        .build())
                .get()
                .header("X-Access-Token", token)
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
                        val userResponse = async(CommonPool) {
                            val loginResponseBean = login(username.text.toString(), HashUtils.md5(password.text.toString()))
                            user(loginResponseBean.accessToken)
                        }
                        try {
                            val userResponseBean = userResponse.await()
                            toast(userResponseBean.oid)
                        } catch (e: Exception) {
                            toast(e.message?:"Unknown Error")
                        }
                    }
                }
            }
        }
    }
}

fun <T> getResponse(responseBody: String, clazz: Class<T>): T {
    val json = JSONObject(responseBody)
    val err = json.optString("err")
    if (TextUtils.isEmpty(err)) {
        return Gson().fromJson(responseBody, clazz)
    }

    throw RuntimeException(err)
}

data class ErrorResponse(val err: String?)
data class LoginRequestBean(val username: String, val password: String)
data class LoginResponseBean(
        @SerializedName("access_token") val accessToken: String,
        @SerializedName("refresh_token") val refreshToken: String
)

data class UserResponseBean(val oid: String) : Serializable