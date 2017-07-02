package com.walfud.oauth2_android_lib.util

import android.arch.lifecycle.LiveData
import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.CallAdapter
import retrofit2.Callback
import retrofit2.Retrofit
import retrofit2.http.*
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.concurrent.atomic.AtomicBoolean

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


/**
 * LiveData<Response<...>>
 */
class LiveDataCallAdapterFactory : CallAdapter.Factory() {

    override fun get(returnType: Type, annotations: Array<Annotation>, retrofit: Retrofit): CallAdapter<*, *>? {
        if (CallAdapter.Factory.getRawType(returnType) != LiveData::class.java) {
            return null
        }

        try {
            val responseType = CallAdapter.Factory.getParameterUpperBound(0, returnType as ParameterizedType)
            val bodyType = CallAdapter.Factory.getParameterUpperBound(0, responseType as ParameterizedType)
            return LiveDataCallAdapter<Type>(bodyType)
        } catch (e: Exception) {
            throw IllegalArgumentException("interface return type MUST be `LiveData<MyResponse<...>>`")
        }
    }
}

class LiveDataCallAdapter<R>(val bodyType: Type) : CallAdapter<R, LiveData<MyResponse<R>>> {
    override fun responseType() = bodyType

    override fun adapt(call: Call<R>): LiveData<MyResponse<R>> = object : LiveData<MyResponse<R>>() {
        private var started = AtomicBoolean(false)

        override fun onActive() {
            if (started.compareAndSet(false, true)) {
                call.enqueue(object : Callback<R> {
                    override fun onResponse(call: Call<R>, response: retrofit2.Response<R>) {
                        postValue(MyResponse.valueOf(response))
                    }

                    override fun onFailure(call: Call<R>, throwable: Throwable) {
                        postValue(MyResponse.valueOf(throwable))
                    }
                })
            }
        }
    }
}

data class MyResponse<out T>(
        val code: Int,
        val headers: Map<String, List<String>>,
        val body: T?,
        val err: String?
) {
    companion object {
        fun <T> valueOf(retrofit2Response: retrofit2.Response<T>): MyResponse<T> {
            val code = retrofit2Response.code()

            // header
            val headers: MutableMap<String, List<String>> = mutableMapOf()
            for ((key, values) in retrofit2Response.headers().toMultimap()) {
                headers.put(key, values)
            }

            // body or error
            var body: T? = null
            var err: String? = null
            when (code) {
                in 200..299 -> {
                    body = retrofit2Response.body()
                }
                in 300..399 -> {
                    // Do nothing. OkHttp takes care of it
                }
                in 400..499 -> {
                    err = retrofit2Response.errorBody()?.string()
                }

                else -> {
                    throw RuntimeException("code: ($code) NOT supported")
                }
            }

            return MyResponse(code, headers, body, err)
        }

        fun <T> valueOf(throwable: Throwable) = MyResponse<T>(500, mapOf(), null, throwable.message)
    }

    fun isSuccess() = code in 200..299
}