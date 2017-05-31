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
import org.jetbrains.anko.bundleOf
import org.jetbrains.anko.coroutines.experimental.bg

/**
 * Created by walfud on 25/05/2017.
 */

val EXTRA_USERNAME = "EXTRA_USERNAME"
val EXTRA_APPNAME = "EXTRA_APPNAME"
val EXTRA_OID = "EXTRA_OID"
val EXTRA_ACCESS_TOKEN = "EXTRA_ACCESS_TOKEN"
val EXTRA_REFRESH_TOKEN = "EXTRA_REFRESH_TOKEN"

fun startOAuth2ActivityForResult(activity: Activity, requestId: Int) {
    startOAuth2ActivityForResult(activity, requestId, null)
}

fun startOAuth2ActivityForResult(activity: Activity, requestId: Int, clientId: String?) {
    val intent = Intent(activity, OAuth2Activity::class.java)
    clientId?.let {
        intent.putExtra(EXTRA_CLIENT_ID, clientId)
    }
    activity.startActivityForResult(intent, requestId)
}

class OAuth2Activity : BaseActivity() {
    companion object {
        val REQUEST_LOGIN = 1
        val REQUEST_AUTHORIZE = 2
    }

    lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)
        viewModel.err.observe(this, Observer {
            finish(it!!, null)
        })
        viewModel.oauth2LiveData.observe(this, Observer {
            it!!

            finish(null, bundleOf(
                    EXTRA_USERNAME to it.username,
                    EXTRA_APPNAME to it.appName,
                    EXTRA_OID to it.oid,
                    EXTRA_ACCESS_TOKEN to it.accessToken,
                    EXTRA_REFRESH_TOKEN to it.refreshToken
            ))
        })

        if (!isLogin()) {
            // Anonymous
            LoginActivity.startActivityForResult(this, REQUEST_LOGIN)
        } else {
            // Login
            // TODO: 设计持久化系统
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode != Activity.RESULT_OK) {
            viewModel.err.value = data?.getStringExtra(EXTRA_ERROR) ?: resources.getString(R.string.main_user_cancel)
            return
        }

        when (requestCode) {
            REQUEST_LOGIN -> {
                data!!
                val loginResponseBean = data.getSerializableExtra(EXTRA_LOGIN_RESPONSE_BEAN) as LoginResponseBean
                if (!intent.hasExtra(EXTRA_CLIENT_ID)) {
                    // finish
                    viewModel.fetchOAuth2Data(loginResponseBean.accessToken, loginResponseBean.refreshToken)
                } else {
                    AuthorizeActivity.startActivityForResult(this, REQUEST_AUTHORIZE, loginResponseBean.accessToken, intent.getStringExtra(EXTRA_CLIENT_ID))
                }
            }
            REQUEST_AUTHORIZE -> {
                data!!
                val tokenResponseBean = data.getSerializableExtra(EXTRA_TOKEN_RESPONSE_BEAN) as TokenResponseBean
                viewModel.fetchOAuth2Data(tokenResponseBean.accessToken, tokenResponseBean.refreshToken)
            }
        }
    }
}

class MainViewModel : ViewModel() {
    val repository = MainRepository()

    val err: MutableLiveData<String> = MutableLiveData()

    val oauth2LiveData: MutableLiveData<OAuth2Data> = MutableLiveData()
    fun fetchOAuth2Data(accessToken: String, refreshToken: String) {
        launch(UI) {
            oauth2LiveData.value = bg {
                val oauth2Data = repository.fetchOAuth2Data(accessToken, refreshToken)
                repository.saveOAuth2Data(oauth2Data)
                return@bg oauth2Data
            }.await()
        }
    }
}

class MainRepository : BaseRepository() {
    fun fetchOAuth2Data(accessToken: String, refreshToken: String): OAuth2Data {
        val userResponseBase = network.user(accessToken)
        val appResponseBase = network.app(accessToken)
        return OAuth2Data(
                userResponseBase.name,
                appResponseBase.name,
                userResponseBase.oid,
                accessToken,
                refreshToken
        )
    }

    fun saveOAuth2Data(oauth2Data: OAuth2Data) {
        try {
            database.beginTransaction()
            database.userDao().upsert(User(oauth2Data.username))
            database.appDao().upsert(App(oauth2Data.appName))
            database.oauth2Dao().upsert(OAuth2(
                    oauth2Data.username,
                    oauth2Data.appName,
                    oauth2Data.oid,
                    oauth2Data.accessToken,
                    oauth2Data.refreshToken
            ))
            database.setTransactionSuccessful()
        } catch (e: Exception) {

        } finally {
            database.endTransaction()
        }
    }
}

data class OAuth2Data(
        var username: String,
        var appName: String,
        var oid: String,
        var accessToken: String,
        var refreshToken: String
)