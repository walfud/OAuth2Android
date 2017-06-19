package com.walfud.oauth2_android

import android.app.Activity
import android.app.Dialog
import android.arch.lifecycle.*
import android.content.Intent
import android.os.Bundle
import com.walfud.oauth2_android.dagger2.DaggerTokenComponent
import com.walfud.oauth2_android.dagger2.TokenModule
import com.walfud.oauth2_android.retrofit2.MyResponse
import org.jetbrains.anko.*
import org.jetbrains.anko.sdk25.coroutines.onClick
import javax.inject.Inject

/**
 * Created by walfud on 22/05/2017.
 */

val EXTRA_CLIENT_ID = "EXTRA_CLIENT_ID"
val EXTRA_TOKEN_RESPONSE_BEAN = "EXTRA_TOKEN_RESPONSE_BEAN"

class TokenActivity : BaseActivity() {

    companion object {
        fun startActivityForResult(activity: Activity, requestId: Int, token: String, clientId: String) {
            val intent = Intent(activity, TokenActivity::class.java)
            intent.putExtra(EXTRA_ACCESS_TOKEN, token)
            intent.putExtra(EXTRA_CLIENT_ID, clientId)
            activity.startActivityForResult(intent, requestId)
        }
    }

    @Inject lateinit var viewModel: TokenViewModel
    lateinit var dialog: Dialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TokenActivityUI().setContentView(this)

        DaggerTokenComponent.builder()
                .applicationComponent((application as OAuth2Application).component)
                .tokenModule(TokenModule(this))
                .build()
                .inject(this)

        viewModel.tokenLiveData.observe(this, Observer {
            it!!

            when (it.status) {
                Resource.STATUS_SUCCESS -> {
                    finish(null, bundleOf(EXTRA_TOKEN_RESPONSE_BEAN to it.data!!))
                }
                Resource.STATUS_ERROR -> {
                    finish(it.err, null)
                }
                Resource.STATUS_LOADING -> {
                    dialog.setTitle(it.loading)
                    dialog.show()
                }
            }
        })

        dialog = Dialog(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (dialog.isShowing) dialog.dismiss()
    }
}

class TokenActivityUI : AnkoComponent<TokenActivity> {
    override fun createView(ui: AnkoContext<TokenActivity>) = with(ui) {
        verticalLayout {
            button(R.string.oauth2_authorize) {
                onClick {
                    val token = owner.intent.getStringExtra(EXTRA_ACCESS_TOKEN)
                    val clientId = owner.intent.getStringExtra(EXTRA_CLIENT_ID)
                    owner.viewModel.authorize(token, clientId)
                }
            }
        }
    }
}

class TokenViewModel : ViewModel() {
    lateinit var repository: TokenRepository

    val tokenInput = MutableLiveData<TokenInput>()
    val tokenLiveData = MediatorLiveData<Resource<TokenResponseBean>>()
    fun authorize(token: String, clientId: String) {
        tokenInput.value = TokenInput(token, clientId)
        tokenLiveData.addSource(tokenInput, {
            tokenLiveData.removeSource(tokenInput)

            it!!
            val authorize = repository.authorize(it.token, it.clientId)
            tokenLiveData.addSource(authorize, { it ->
                it!!
                when (it.status) {
                    Resource.STATUS_SUCCESS -> {
                        tokenLiveData.removeSource(authorize)

                        it.data!!
                        val token = repository.token(tokenInput.value!!.token,
                                it.data.cb,
                                tokenInput.value!!.clientId,
                                it.data.code)
                        tokenLiveData.addSource(token, {
                            it!!
                            when (it.status) {
                                Resource.STATUS_SUCCESS, Resource.STATUS_ERROR -> {
                                    tokenLiveData.removeSource(token)
                                }
                            }
                            tokenLiveData.value = it
                        })
                    }
                    Resource.STATUS_ERROR -> {
                        tokenLiveData.removeSource(authorize)
                        tokenLiveData.value = Resource.fail<TokenResponseBean>(it.err)
                    }
                    Resource.STATUS_LOADING -> {
                        tokenLiveData.value = Resource.loading<TokenResponseBean>(it.loading)
                    }
                }
            })
        })
    }
}

class TokenRepository(val preference: Preference, val database: Database, val network: Network) {
    fun authorize(token: String, clientId: String): LiveData<Resource<AuthorizeResponseBean>> {
        return object : ResourceFetcher<AuthorizeResponseBean>("fetching code") {
            override fun network(): LiveData<MyResponse<AuthorizeResponseBean>> {
                return network.authorize(token, clientId)
            }
        }
                .fetch()
                .asLiveData()
    }

    fun token(token: String, url: String, clientId: String, code: String): LiveData<Resource<TokenResponseBean>> {
        return object : ResourceFetcher<TokenResponseBean>("fetching token") {
            override fun network(): LiveData<MyResponse<TokenResponseBean>> {
                return network.token(token, url, clientId, code)
            }
        }
                .fetch()
                .asLiveData()
    }
}

data class TokenInput(val token: String, val clientId: String)