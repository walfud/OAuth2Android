package com.walfud.oauth2_android

import android.text.TextUtils
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import org.json.JSONObject
import java.io.Serializable

/**
 * Created by walfud on 25/05/2017.
 */

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