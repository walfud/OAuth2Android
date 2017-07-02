package com.walfud.oauth2_android_lib.activity

import android.app.Activity
import android.app.Dialog
import android.arch.lifecycle.*
import android.content.Intent
import android.os.Bundle
import com.walfud.oauth2_android_lib.OAuth2
import com.walfud.oauth2_android_lib.R
import com.walfud.oauth2_android_lib.util.*
import com.walfud.walle.md5
import org.jetbrains.anko.*
import org.jetbrains.anko.sdk25.coroutines.onClick

val EXTRA_LOGIN_RESPONSE_BEAN = "EXTRA_LOGIN_RESPONSE_BEAN"

class LoginActivity : BaseActivity() {
    companion object {
        fun startActivityForResult(activity: Activity, requestId: Int) {
            val intent = Intent(activity, LoginActivity::class.java)
            activity.startActivityForResult(intent, requestId)
        }
    }

    lateinit var viewModel: LoginViewModel
    lateinit var dialog: Dialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LoginActivityUI().setContentView(this)

        viewModel = ViewModelProviders.of(this).get(LoginViewModel::class.java)
        viewModel.repository = LoginRepository(OAuth2.network)
        dialog = Dialog(this)

        viewModel.loginLiveData.observe(this, Observer {
            it!!

            when (it.status) {
                Resource.STATUS_SUCCESS -> {
                    finish(null, bundleOf(EXTRA_LOGIN_RESPONSE_BEAN to it.data!!))
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

class LoginActivityUI : AnkoComponent<LoginActivity> {
    override fun createView(ui: AnkoContext<LoginActivity>) = with(ui) {
        verticalLayout {
            val usernameEt = editText()
            val passwordEt = editText()
            button(R.string.login_login) {
                onClick {
                    val username = usernameEt.text.toString()
                    val password = passwordEt.text.toString().md5()
                    owner.viewModel.login(username, password)
                }
            }
        }
    }
}

class LoginViewModel : ViewModel() {
    lateinit var repository: LoginRepository

    val loginInputLiveData = MutableLiveData<LoginRequestBean>()
    fun login(username: String, password: String) {
        loginInputLiveData.value = LoginRequestBean(username, password)
    }

    val loginLiveData: LiveData<Resource<LoginResponseBean>> = Transformations.switchMap(loginInputLiveData, { (username, password) ->
        repository.login(username, password)
    })
}

class LoginRepository(val network: Network) {
    fun login(username: String, password: String): LiveData<Resource<LoginResponseBean>> {
        return object : ResourceFetcher<LoginResponseBean>("network: login") {
            override fun network(): LiveData<MyResponse<LoginResponseBean>> {
                return network.login(LoginRequestBean(username, password))
            }
        }
                .fetch()
                .asLiveData()
    }
}