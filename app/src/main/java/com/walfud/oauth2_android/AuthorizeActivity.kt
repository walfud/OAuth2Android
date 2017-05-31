package com.walfud.oauth2_android

import android.app.Activity
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProviders
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

val EXTRA_CLIENT_ID = "EXTRA_CLIENT_ID"
val EXTRA_TOKEN_RESPONSE_BEAN = "EXTRA_TOKEN_RESPONSE_BEAN"

class AuthorizeActivity : BaseActivity() {

    companion object {
        fun startActivityForResult(activity: Activity, requestId: Int, token: String, clientId: String) {
            val intent = Intent(activity, AuthorizeActivity::class.java)
            intent.putExtra(EXTRA_ACCESS_TOKEN, token)
            intent.putExtra(EXTRA_CLIENT_ID, clientId)
            activity.startActivityForResult(intent, requestId)
        }
    }

    lateinit var viewModel: AuthorizeViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        OAuth2ActivityUI().setContentView(this)

        viewModel = ViewModelProviders.of(this).get(AuthorizeViewModel::class.java)
        viewModel.err.observe(this, Observer {
            finish(it!!, null)
        })
        viewModel.tokenLiveData.observe(this, Observer {
            finish(null, bundleOf(EXTRA_TOKEN_RESPONSE_BEAN to it!!))
        })
    }

    fun onOAuth2() {
        val token = intent.getStringExtra(EXTRA_ACCESS_TOKEN)
        val clientId = intent.getStringExtra(EXTRA_CLIENT_ID)
        viewModel.authorize(token, clientId)
    }
}

class OAuth2ActivityUI : AnkoComponent<AuthorizeActivity> {
    override fun createView(ui: AnkoContext<AuthorizeActivity>) = with(ui) {
        verticalLayout {
            button(R.string.oauth2_authorize) {
                onClick {
                    owner.onOAuth2()
                }
            }
        }
    }
}

class AuthorizeViewModel : ViewModel() {
    val repository = AuthorizeRepository()

    val err = MutableLiveData<String>()
    val tokenLiveData = MutableLiveData<TokenResponseBean>()
    fun authorize(username: String, password: String) {
        launch(UI) {
            try {
                val tokenResponseBean = bg {
                    repository.token(username, password)
                }.await()

                tokenLiveData.value = tokenResponseBean
            } catch (e: Exception) {
                err.value = e.message
            }
        }
    }
}

class AuthorizeRepository : BaseRepository() {
    fun token(token: String, clientId: String): TokenResponseBean {
        val authorizeResponseBean = network.authorize(token, clientId)
        return network.token(token, clientId, Uri.parse(authorizeResponseBean.cb), authorizeResponseBean.code)
    }
}