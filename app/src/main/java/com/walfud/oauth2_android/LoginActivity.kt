package com.walfud.oauth2_android

import android.app.Activity
import android.arch.lifecycle.*
import android.content.Intent
import android.os.Bundle
import com.walfud.oauth2_android.retrofit2.MyResponse
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LoginActivityUI().setContentView(this)

        viewModel = ViewModelProviders.of(this).get(LoginViewModel::class.java)
        viewModel.err.observe(this, Observer {
            finish(it!!, null)
        })
        viewModel.loginLiveData.observe(this, Observer {
            finish(null, bundleOf(EXTRA_LOGIN_RESPONSE_BEAN to it!!.body!!))
        })
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
                    val password = passwordEt.text.toString()
                    owner.viewModel.login(username, password)
                }
            }
        }
    }
}

class LoginViewModel : ViewModel() {
    val repository = LoginRepository()

    val err = MutableLiveData<String>()
    val loginInput = MutableLiveData<LoginRequestBean>()
    val loginLiveData: LiveData<MyResponse<LoginResponseBean>> = Transformations.switchMap(loginInput, { (username, password) ->
        repository.login(username, password)
    })
    fun login(username: String, password: String) {
        loginInput.value = LoginRequestBean(username, password)
    }
}

class LoginRepository : BaseRepository() {
    fun login(username: String, password: String): LiveData<MyResponse<LoginResponseBean>> {
        return network.login(LoginRequestBean(username, password))
    }
}