package com.walfud.oauth2_android.oauth2

import android.app.Activity
import android.app.Dialog
import android.arch.lifecycle.*
import android.content.Intent
import android.os.Bundle
import com.walfud.oauth2_android.*
import com.walfud.oauth2_android_lib.R
import org.jetbrains.anko.*
import org.jetbrains.anko.sdk25.coroutines.onClick
import javax.inject.Inject

/**
 * Created by walfud on 22/05/2017.
 */

val EXTRA_CLIENT_ID = "EXTRA_CLIENT_ID"
val EXTRA_AUTHORIZE_RESPONSE_BEAN = "EXTRA_AUTHORIZE_RESPONSE_BEAN"

class AuthorizeActivity : BaseActivity() {

    companion object {
        fun startActivityForResult(activity: Activity, requestId: Int, token: String, clientId: String) {
            val intent = Intent(activity, AuthorizeActivity::class.java)
            intent.putExtra(EXTRA_ACCESS_TOKEN, token)
            intent.putExtra(EXTRA_CLIENT_ID, clientId)
            activity.startActivityForResult(intent, requestId)
        }
    }

    @Inject lateinit var viewModel: AuthorizeViewModel
    lateinit var dialog: Dialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AuthorizeActivityUI().setContentView(this)

        dialog = Dialog(this)

        viewModel.authorizeLiveData.observe(this, Observer {
            it!!

            when (it.status) {
                Resource.STATUS_SUCCESS -> {
                    finish(null, bundleOf(EXTRA_AUTHORIZE_RESPONSE_BEAN to it.data!!))
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
    }

    override fun onDestroy() {
        super.onDestroy()
        if (dialog.isShowing) dialog.dismiss()
    }
}

class AuthorizeActivityUI : AnkoComponent<AuthorizeActivity> {
    override fun createView(ui: AnkoContext<AuthorizeActivity>) = with(ui) {
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

class AuthorizeViewModel : ViewModel() {
    lateinit var repository: AuthorizeRepository

    val authorizeInputLiveData = MutableLiveData<AuthorizeInput>()
    val authorizeLiveData = MediatorLiveData<Resource<AuthorizeResponseBean>>()
    fun authorize(token: String, clientId: String) {
        authorizeInputLiveData.value = AuthorizeInput(token, clientId)
        authorizeLiveData.addSource(authorizeInputLiveData, { authorizeInput ->
            authorizeLiveData.removeSource(authorizeInputLiveData)

            authorizeInput!!
            val authorize = repository.authorize(authorizeInput.token, authorizeInput.clientId)
            authorizeLiveData.addSource(authorize, { authorizeResponse ->
                authorizeResponse!!
                when (authorizeResponse.status) {
                    Resource.STATUS_SUCCESS -> {
                        authorizeLiveData.removeSource(authorize)
                        authorizeLiveData.value = Resource.success(authorizeResponse.data)
                    }
                    Resource.STATUS_ERROR -> {
                        authorizeLiveData.removeSource(authorize)
                        authorizeLiveData.value = Resource.fail<AuthorizeResponseBean>(authorizeResponse.err)
                    }
                    Resource.STATUS_LOADING -> {
                        authorizeLiveData.value = Resource.loading<AuthorizeResponseBean>(authorizeResponse.loading)
                    }
                }
            })
        })
    }
}

class AuthorizeRepository(val network: Network) {
    fun authorize(token: String, clientId: String): LiveData<Resource<AuthorizeResponseBean>> {
        return object : ResourceFetcher<AuthorizeResponseBean>("network: code") {
            override fun network(): LiveData<MyResponse<AuthorizeResponseBean>> {
                return network.authorize(token, clientId)
            }
        }
                .fetch()
                .asLiveData()
    }
}

data class AuthorizeInput(val token: String, val clientId: String)