package com.walfud.oauth2_android

import android.app.Activity
import android.arch.lifecycle.*
import android.content.Intent
import android.os.Bundle
import com.walfud.oauth2_android.dagger2.AuthorizeModule
import com.walfud.oauth2_android.dagger2.DaggerAuthorizeComponent
import com.walfud.oauth2_android.retrofit2.MyResponse
import org.jetbrains.anko.*
import org.jetbrains.anko.sdk25.coroutines.onClick
import javax.inject.Inject

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

    @Inject lateinit var viewModel: AuthorizeViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        OAuth2ActivityUI().setContentView(this)

        DaggerAuthorizeComponent.builder()
                .applicationComponent((application as OAuth2Application).component)
                .authorizeModule(AuthorizeModule(this))
                .build()
                .inject(this)

        viewModel = ViewModelProviders.of(this).get(AuthorizeViewModel::class.java)
        viewModel.err.observe(this, Observer {
            finish(it!!, null)
        })
        viewModel.tokenLiveData.observe(this, Observer {
            finish(null, bundleOf(EXTRA_TOKEN_RESPONSE_BEAN to it!!.body!!))
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
    lateinit var repository: AuthorizeRepository

    val err = MutableLiveData<String>()
    val authorizeInput = MutableLiveData<AuthorizeInput>()
    val authorizeLiveData: LiveData<MyResponse<AuthorizeResponseBean>> = Transformations.switchMap(authorizeInput, { (token, clientId) ->
        repository.authorize(token, clientId)
    })
    fun authorize(token: String, clientId: String) {
        authorizeInput.value = AuthorizeInput(token, clientId)
    }

    val tokenLiveData: LiveData<MyResponse<TokenResponseBean>> = Transformations.switchMap(authorizeLiveData, { myResponse ->
        if (!myResponse.isSuccess()) {
            err.value = myResponse.err
            return@switchMap MutableLiveData<MyResponse<TokenResponseBean>>()
        }
        repository.token(authorizeInput.value!!.token,
                myResponse.body!!.cb,
                authorizeInput.value!!.clientId,
                myResponse.body!!.code)
    })
}

class AuthorizeRepository(val preference: Preference, val database: Database, val network: Network) {
    fun authorize(token: String, clientId: String): LiveData<MyResponse<AuthorizeResponseBean>> {
        return network.authorize(token, clientId)
    }

    fun token(token: String, url: String, clientId: String, code: String): LiveData<MyResponse<TokenResponseBean>> {
        return network.token(token, url, clientId, code)
    }
}

data class AuthorizeInput(val token: String, val clientId: String)