package com.walfud.oauth2_android

import android.arch.lifecycle.LiveData
import com.google.gson.annotations.SerializedName
import com.walfud.oauth2_android.retrofit2.MyResponse
import retrofit2.http.*

/**
 * Created by walfud on 25/05/2017.
 */


const val HEADER_ACCESS_TOKEN = "X-Access-Token"

interface Network {

    @POST("login")
    fun login(@Body loginRequestBean: LoginRequestBean): LiveData<MyResponse<LoginResponseBean>>

    @GET("authorize?response_type=code&redirect_uri=&scope=&state=")
    fun authorize(@Header(HEADER_ACCESS_TOKEN) token: String,
                  @Query("client_id") clientId: String): LiveData<MyResponse<AuthorizeResponseBean>>

    @FormUrlEncoded
    @POST
    fun token(@Header(HEADER_ACCESS_TOKEN) token: String,
              @Url url: String,
              @Field("client_id") clientId: String,
              @Field("code") code: String,
              @Field("grant_type") grantType: String = "authorization_code",
              @Field("redirect_uri") redirectUri: String = ""
              ): LiveData<MyResponse<TokenResponseBean>>

    @GET("user")
    fun user(@Header(HEADER_ACCESS_TOKEN) token: String): LiveData<MyResponse<UserResponseBean>>

    @GET("app")
    fun app(@Header(HEADER_ACCESS_TOKEN) token: String): LiveData<MyResponse<AppResponseBean>>

}

data class LoginRequestBean(val username: String, val password: String)
data class LoginResponseBean(
        @SerializedName("access_token") val accessToken: String,
        @SerializedName("refresh_token") val refreshToken: String
) : java.io.Serializable

data class AuthorizeResponseBean(
        val code: String,
        val state: String,
        val cb: String
) : java.io.Serializable

data class TokenResponseBean(
        @SerializedName("access_token") val accessToken: String,
        @SerializedName("refresh_token") val refreshToken: String,
        @SerializedName("expires_in") val expiresIn: Long,
        @SerializedName("token_type") val tokenType: String
) : java.io.Serializable

data class UserResponseBean(
        val oid: String,
        val name: String
) : java.io.Serializable

data class AppResponseBean(
        val oid: String,
        val name: String
) : java.io.Serializable
