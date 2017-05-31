package com.walfud.oauth2_android

import android.app.Activity
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.*
import org.jetbrains.anko.coroutines.experimental.bg
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
            finish(null, bundleOf(EXTRA_LOGIN_RESPONSE_BEAN to it!!))
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
    val loginLiveData = MutableLiveData<LoginResponseBean>()
    fun login(username: String, password: String) {
        launch(UI) {
            try {
                val loginResponseBean = bg {
                    repository.login(username, password)
                }.await()

                loginLiveData.value = loginResponseBean
            } catch (e: Exception) {
                err.value = e.message
            }
        }
    }
}

class LoginRepository : BaseRepository() {
    fun login(username: String, password: String): LoginResponseBean {
        return network.login(username, password)
    }
}