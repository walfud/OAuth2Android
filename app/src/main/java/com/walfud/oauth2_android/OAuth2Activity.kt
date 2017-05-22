package com.walfud.oauth2_android

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.Request
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.coroutines.experimental.bg
import org.jetbrains.anko.progressDialog
import java.io.Serializable

/**
 * Created by walfud on 22/05/2017.
 */

class OAuth2Activity : Activity(), AnkoLogger {

    companion object {
        val EXTRA_CLIENT_ID: String = "EXTRA_CLIENT_ID"
        val ID_REQUEST = 1
        val EXTRA_TOKEN_RESPONSE_BEAN: String = "EXTRA_TOKEN_RESPONSE_BEAN"

        fun startActivityForResult(activity: Activity, clientId: String) {
            val intent = Intent(activity, OAuth2Activity::class.java)
            intent.putExtra(EXTRA_CLIENT_ID, clientId)
            activity.startActivityForResult(intent, ID_REQUEST)
        }
    }

    private var mDialog: Dialog? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        progressDialog(message = "a moment please...")
        launch(UI) {
            val oauth = bg {
                val clientId = intent.getStringExtra(EXTRA_CLIENT_ID)
                val authorizeResponseBean = authorize(clientId)
                token(clientId, Uri.parse(authorizeResponseBean.cb), authorizeResponseBean.code)
            }.await()
            finish(oauth)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mDialog?.dismiss()
    }

    fun authorize(clientId: String, redirectUri: Uri = Uri.EMPTY, scope: String = "", state: String = ""): AuthorizeResponseBean {
        val authorizeResponse = OAuth2Application.okHttpClient.newCall(Request.Builder()
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
                .get()
                .build())
                .execute()
        if (!authorizeResponse.isSuccessful) throw RuntimeException("authorize fail")

        return Gson().fromJson(authorizeResponse.body().string(), AuthorizeResponseBean::class.java)
    }

    fun token(clientId: String, redirectUri: Uri = Uri.EMPTY, code: String): TokenResponseBean {
        val tokenResponse = OAuth2Application.okHttpClient.newCall(Request.Builder()
                .url(redirectUri.toString())
                .post(FormBody.Builder()
                        .add("grant_type", "authorization_code")
                        .add("client_id", clientId)
                        .add("redirect_uri", redirectUri.toString())
                        .add("code", code)
                        .build())
                .build())
                .execute()
        if (!tokenResponse.isSuccessful) throw RuntimeException("token fail")

        return Gson().fromJson(tokenResponse.body().string(), TokenResponseBean::class.java)
    }


    fun finish(tokenResponseBean: TokenResponseBean) {
        val intent = Intent()
        intent.putExtra(EXTRA_TOKEN_RESPONSE_BEAN, tokenResponseBean)
        setResult(RESULT_OK, intent)
        finish()
    }
}

data class AuthorizeResponseBean(
        val code: String,
        val state: String,
        val cb: String
)

data class TokenResponseBean(
        @SerializedName("access_token") val accessToken: String,
        @SerializedName("refresh_token") val refreshToken: String,
        @SerializedName("expires_in") val expiresIn: Long,
        @SerializedName("token_type") val tokenType: String
) : Serializable