package com.walfud.oauth2_android

import android.net.Uri
import android.text.TextUtils
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.walfud.walle.algorithm.hash.HashUtils
import okhttp3.*
import org.json.JSONObject
import java.io.Serializable

/**
 * Created by walfud on 25/05/2017.
 */

val network by lazy { Network() }

class Network {

    companion object {
        val HEADER_ACCESS_TOKEN = "X-Access-Token"
    }

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
                .build()
    }

    fun login(username: String, password: String): LoginResponseBean {
        val loginRequestBean = LoginRequestBean(username,
                HashUtils.md5(password))
        val loginResponse = okHttpClient.newCall(Request.Builder()
                .url("http://oauth2.walfud.com/login")
                .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"),
                        Gson().toJson(loginRequestBean)))
                .build())
                .execute()
        if (!loginResponse.isSuccessful) throw RuntimeException("login fail")

        return getResponse(loginResponse.body().string(), LoginResponseBean::class.java)
    }

    fun authorize(token: String, clientId: String, redirectUri: Uri = Uri.EMPTY, scope: String = "", state: String = ""): AuthorizeResponseBean {
        val authorizeResponse = okHttpClient.newCall(Request.Builder()
                .url(HttpUrl.Builder()
                        .scheme("http")
                        .host("oauth2.walfud.com")
                        .addPathSegment("authorize")
                        .addQueryParameter("response_type", "code")
                        .addQueryParameter("client_id", clientId)
                        .addQueryParameter("redirect_uri", redirectUri.toString())
                        .addQueryParameter("scope", scope)
                        .addQueryParameter("state", state)
                        .build())
                .header(HEADER_ACCESS_TOKEN, token)
                .get()
                .build())
                .execute()
        if (!authorizeResponse.isSuccessful) throw RuntimeException("authorize fail")

        return getResponse(authorizeResponse.body().string(), AuthorizeResponseBean::class.java)
    }

    fun token(token: String, clientId: String, redirectUri: Uri = Uri.EMPTY, code: String): TokenResponseBean {
        val tokenResponse = okHttpClient.newCall(Request.Builder()
                .url(redirectUri.toString())
                .header(HEADER_ACCESS_TOKEN, token)
                .post(FormBody.Builder()
                        .add("grant_type", "authorization_code")
                        .add("client_id", clientId)
                        .add("redirect_uri", redirectUri.toString())
                        .add("code", code)
                        .build())
                .build())
                .execute()
        if (!tokenResponse.isSuccessful) throw RuntimeException("token fail")

        return getResponse(tokenResponse.body().string(), TokenResponseBean::class.java)
    }


    fun user(token: String): UserResponseBean {
        val userResponse = okHttpClient.newCall(Request.Builder()
                .url(HttpUrl.Builder()
                        .scheme("http")
                        .host("oauth2.walfud.com")
                        .addPathSegment("user")
                        .build())
                .header(HEADER_ACCESS_TOKEN, token)
                .get()
                .build())
                .execute()
        if (!userResponse.isSuccessful) throw RuntimeException("fetch user fail")

        return getResponse(userResponse.body().string(), UserResponseBean::class.java)
    }

    fun app(token: String): AppResponseBean {
        val userResponse = okHttpClient.newCall(Request.Builder()
                .url(HttpUrl.Builder()
                        .scheme("http")
                        .host("oauth2.walfud.com")
                        .addPathSegment("app")
                        .build())
                .header(HEADER_ACCESS_TOKEN, token)
                .get()
                .build())
                .execute()
        if (!userResponse.isSuccessful) throw RuntimeException("fetch app fail")

        return getResponse(userResponse.body().string(), AppResponseBean::class.java)
    }

    fun <T> getResponse(responseBody: String, clazz: Class<T>): T {
        val json = JSONObject(responseBody)
        val err = json.optString("err")
        if (TextUtils.isEmpty(err)) {
            return Gson().fromJson(responseBody, clazz)
        }

        throw RuntimeException(err)
    }
}

data class ErrorResponse(val err: String?)
data class LoginRequestBean(val username: String, val password: String)
data class LoginResponseBean(
        @SerializedName("access_token") val accessToken: String,
        @SerializedName("refresh_token") val refreshToken: String
) : Serializable

data class AuthorizeResponseBean(
        val code: String,
        val state: String,
        val cb: String
) : Serializable

data class TokenResponseBean(
        @SerializedName("access_token") val accessToken: String,
        @SerializedName("refresh_token") val refreshToken: String,
        @SerializedName("expires_in") val expiresIn: Long,
        @SerializedName("token_type") val tokenType: String
) : Serializable

data class UserResponseBean(
        val oid: String,
        @SerializedName("username") val name: String
) : Serializable

data class AppResponseBean(
        val oid: String,
        @SerializedName("app_name") val name: String
) : Serializable
