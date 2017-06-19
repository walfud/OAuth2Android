package com.walfud.oauth2_android

import android.app.Activity
import android.arch.lifecycle.*
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import com.walfud.oauth2_android.OAuth2Activity.Companion.REQUEST_LOGIN
import com.walfud.oauth2_android.dagger2.DaggerOAuth2Component
import com.walfud.oauth2_android.dagger2.OAuth2Module
import com.walfud.oauth2_android.retrofit2.MyResponse
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import org.jetbrains.anko.bundleOf
import javax.inject.Inject

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

    @Inject lateinit var viewModel: OAuth2ViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        DaggerOAuth2Component.builder()
                .applicationComponent((application as OAuth2Application).component)
                .oAuth2Module(OAuth2Module(this))
                .build()
                .inject(this)

        viewModel = ViewModelProviders.of(this).get(OAuth2ViewModel::class.java)
        viewModel.err.observe(this, Observer {
            finish(it!!, null)
        })
        viewModel.fetchUserLiveData.observe(this, Observer {
            it!!

            finish(null, bundleOf(
                    EXTRA_USERNAME to it.username,
                    EXTRA_APPNAME to it.appName,
                    EXTRA_OID to it.oid,
                    EXTRA_ACCESS_TOKEN to it.accessToken,
                    EXTRA_REFRESH_TOKEN to it.refreshToken
            ))
        })
        viewModel.authorizeInput.observe(this, Observer {
            it!!

            TokenActivity.startActivityForResult(this, REQUEST_AUTHORIZE, it, intent.getStringExtra(EXTRA_CLIENT_ID))
        })
        viewModel.queryUserDataLiveData.observe(this, Observer {
            it!!

            finish(null, bundleOf(
                    EXTRA_USERNAME to it.username,
                    EXTRA_APPNAME to it.appName,
                    EXTRA_OID to it.oid,
                    EXTRA_ACCESS_TOKEN to it.accessToken,
                    EXTRA_REFRESH_TOKEN to it.refreshToken
            ))
        })
        viewModel.loginLiveData.observe(this, Observer {
            it!!
            LoginActivity.startActivityForResult(this, it)
        })

        viewModel.param(intent.extras)
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
                    viewModel.fetchUserData(loginResponseBean.accessToken, loginResponseBean.refreshToken, true)
                } else {
                    viewModel.authorizeInput.value = loginResponseBean.accessToken
                }
            }
            REQUEST_AUTHORIZE -> {
                data!!
                val tokenResponseBean = data.getSerializableExtra(EXTRA_TOKEN_RESPONSE_BEAN) as TokenResponseBean
                viewModel.fetchUserData(tokenResponseBean.accessToken, tokenResponseBean.refreshToken, false)
            }
        }
    }
}

class OAuth2ViewModel : ViewModel() {
    lateinit var repository: OAuth2Repository

    val err: MutableLiveData<String> = MutableLiveData()

    val loginLiveData = MutableLiveData<Int>()
    fun param(bundle: Bundle?) {
        if (TextUtils.isEmpty(repository.preference.oid)) {
            loginLiveData.value = REQUEST_LOGIN
        } else {
            if (bundle?.containsKey(EXTRA_CLIENT_ID) ?: false) {
                queryToken()
            } else {
                queryUserDataInput.value = repository.preference.oid!!
            }
        }
    }

    val fetchUserInput = MutableLiveData<FetchUserInput>()
    val userLiveData: LiveData<MyResponse<UserResponseBean>> = Transformations.switchMap(fetchUserInput, { (accessToken, refreshToken) ->
        repository.user(accessToken)
    })
    val appLiveData: LiveData<MyResponse<AppResponseBean>> = Transformations.switchMap(userLiveData, { userResponse ->
        if (!userResponse.isSuccess()) {
            err.value = userResponse.err
            return@switchMap MutableLiveData<MyResponse<AppResponseBean>>()
        }
        repository.app(fetchUserInput.value!!.accessToken)
    })
    val fetchUserLiveData: LiveData<OAuth2Data> = Transformations.switchMap(appLiveData, { appResponse ->
        if (!appResponse.isSuccess()) {
            err.value = appResponse.err
            return@switchMap MutableLiveData<OAuth2Data>()
        }

        if (fetchUserInput.value!!.savePrefs) repository.preference.oid = userLiveData.value!!.body!!.oid

        val oauth2Data = OAuth2Data(
                userLiveData.value!!.body!!.name,
                appLiveData.value!!.body!!.name,
                userLiveData.value!!.body!!.oid,
                fetchUserInput.value!!.accessToken,
                fetchUserInput.value!!.refreshToken
        )
        repository.saveOAuth2Data(oauth2Data)
    })

    fun fetchUserData(accessToken: String, refreshToken: String, savePrefs: Boolean) {
        fetchUserInput.value = FetchUserInput(accessToken, refreshToken, savePrefs)
    }

    val authorizeInput = MutableLiveData<String>()
    fun queryToken() {
        async(CommonPool) {
            val oauth2 = repository.database.oauth2Dao().querySync(repository.preference.oid!!)
            authorizeInput.postValue(oauth2.accessToken)
        }
    }

    val queryUserDataInput = MutableLiveData<String>()
    val queryOAuth2 = Transformations.switchMap(queryUserDataInput, {
        repository.database.oauth2Dao().query(it!!)
    })!!
    val queryUser = Transformations.switchMap(queryOAuth2, {
        repository.database.userDao().query(queryOAuth2.value!!.user_name!!)
    })!!
    val queryApp = Transformations.switchMap(queryUser, {
        repository.database.appDao().query(queryOAuth2.value!!.app_name!!)
    })!!
    val queryUserDataLiveData = Transformations.map(queryApp, {
        OAuth2Data(
                queryUser.value!!.name!!,
                queryApp.value!!.name!!,
                queryOAuth2.value!!.oid!!,
                queryOAuth2.value!!.accessToken!!,
                queryOAuth2.value!!.refreshToken!!
        )
    })!!
}

class OAuth2Repository(val preference: Preference, val database: Database, val network: Network) {

    fun user(token: String): LiveData<MyResponse<UserResponseBean>> {
        return network.user(token)
    }

    fun app(token: String): LiveData<MyResponse<AppResponseBean>> {
        return network.app(token)
    }

    fun saveOAuth2Data(oauth2Data: OAuth2Data): LiveData<OAuth2Data> {
        val liveData = MutableLiveData<OAuth2Data>()
        async(CommonPool) {
            database.beginTransaction()
            try {
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
                liveData.postValue(oauth2Data)
            } catch (e: Exception) {
                e.printStackTrace()
                liveData.postValue(null)
            } finally {
                database.endTransaction()
            }
        }
        return liveData
    }
}

data class OAuth2Data(
        var username: String,
        var appName: String,
        var oid: String,
        var accessToken: String,
        var refreshToken: String
)

data class FetchUserInput(
        val accessToken: String,
        val refreshToken: String,
        val savePrefs: Boolean
)