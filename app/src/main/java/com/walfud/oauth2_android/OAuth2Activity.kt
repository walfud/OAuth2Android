package com.walfud.oauth2_android

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.*
import org.jetbrains.anko.coroutines.experimental.bg
import org.jetbrains.anko.sdk25.coroutines.onClick

/**
 * Created by walfud on 22/05/2017.
 */

class OAuth2Activity : BaseActivity() {

    companion object {
        val EXTRA_CLIENT_ID: String = "EXTRA_CLIENT_ID"
        val EXTRA_TOKEN_RESPONSE_BEAN: String = "EXTRA_TOKEN_RESPONSE_BEAN"

        fun startActivityForResult(activity: Activity, requestId: Int, clientId: String) {
            val intent = Intent(activity, OAuth2Activity::class.java)
            intent.putExtra(EXTRA_CLIENT_ID, clientId)
            activity.startActivityForResult(intent, requestId)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        OAuth2ActivityUI().setContentView(this)
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    fun performOAuth2() {
        val dialog = progressDialog(message = "OAuthorizing...")
        launch(UI) {
            try {
                val oauth = bg {
                    val clientId = intent.getStringExtra(EXTRA_CLIENT_ID)
                    val authorizeResponseBean = authorize(getToken(), clientId)
                    token(getToken(), clientId, Uri.parse(authorizeResponseBean.cb), authorizeResponseBean.code)
                }.await()
                finish(null, bundleOf(EXTRA_TOKEN_RESPONSE_BEAN to oauth))
            } catch (e: Exception) {
                finish(e.message, null)
            } finally {
                dialog.dismiss()
            }
        }
    }
}

class OAuth2ActivityUI : AnkoComponent<OAuth2Activity> {
    override fun createView(ui: AnkoContext<OAuth2Activity>) = with(ui) {
        verticalLayout {
            button(R.string.oauth2_authorize) {
                onClick {
                    with(ui.owner) {
                        performOAuth2()
                    }
                }
            }
        }
    }
}