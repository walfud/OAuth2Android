package com.walfud.oauth2_android.oauth2

import android.app.Activity
import android.app.Dialog
import android.arch.lifecycle.*
import android.content.Intent
import android.os.Bundle
import com.walfud.oauth2_android.*
import com.walfud.oauth2_android_lib.R
import org.jetbrains.anko.bundleOf
import javax.inject.Inject

/**
 * Created by walfud on 25/05/2017.
 */

val EXTRA_ERROR = "EXTRA_ERROR"
val EXTRA_USERNAME = "EXTRA_USERNAME"
val EXTRA_APPNAME = "EXTRA_APPNAME"
val EXTRA_OID = "EXTRA_OID"
val EXTRA_ACCESS_TOKEN = "EXTRA_ACCESS_TOKEN"
val EXTRA_REFRESH_TOKEN = "EXTRA_REFRESH_TOKEN"

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
        val REQUEST_TOKEN = 2
    }

    @Inject lateinit var viewModel: OAuth2ViewModel
    lateinit var dialog: Dialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        DaggerActivityComponent.builder()
                .applicationComponent(OAuth2Application.component)
                .build()
                .inject(this)
        dialog = Dialog(this)

        viewModel.loginInputLiveData.observe(this, Observer {
            it!!
            LoginActivity.startActivityForResult(this, it.requestId)
        })
        viewModel.authorizeInputLiveData.observe(this, Observer {
            it!!
            AuthorizeActivity.startActivityForResult(this, it.requestId, it.token, it.clientId)
        })
        viewModel.oauth2LiveData.observe(this, Observer {
            it!!

            when (it.status) {
                Resource.STATUS_SUCCESS -> {
                    it.data!!
                    finish(null, bundleOf(
                            EXTRA_USERNAME to it.data.username,
                            EXTRA_APPNAME to it.data.appName,
                            EXTRA_OID to it.data.oid,
                            EXTRA_ACCESS_TOKEN to it.data.accessToken,
                            EXTRA_REFRESH_TOKEN to it.data.refreshToken
                    ))
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

        viewModel.dispatch(intent.extras)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (dialog.isShowing) dialog.dismiss()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode != Activity.RESULT_OK) {
            viewModel.oauth2LiveData.value = Resource.fail(data?.getStringExtra(EXTRA_ERROR) ?: resources.getString(R.string.main_user_cancel))
            return
        }

        when (requestCode) {
            REQUEST_LOGIN -> {
                data!!
                val loginResponseBean = data.getSerializableExtra(EXTRA_LOGIN_RESPONSE_BEAN) as LoginResponseBean
                viewModel.setLoginResponse(loginResponseBean)
            }
            REQUEST_TOKEN -> {
                data!!
                val tokenResponseBean = data.getSerializableExtra(EXTRA_AUTHORIZE_RESPONSE_BEAN) as AuthorizeResponseBean
                viewModel.setAuthorizeResponseBean(tokenResponseBean)
            }
        }
    }
}

class OAuth2ViewModel : ViewModel() {
    lateinit var repository: OAuth2Repository

    val oauth2LiveData = MediatorLiveData<Resource<OAuth2ViewData>>()
    val loginInputLiveData = MutableLiveData<StartLoginInput>()
    val authorizeInputLiveData = MutableLiveData<StartTokenInput>()
    var bundle: Bundle? = null
    fun dispatch(bundle: Bundle?) {
        this.bundle = bundle

        loginInputLiveData.value = StartLoginInput(OAuth2Activity.REQUEST_LOGIN)
    }

    fun setLoginResponse(loginResponseBean: LoginResponseBean) {
        authorizeInputLiveData.value = StartTokenInput(OAuth2Activity.REQUEST_TOKEN, loginResponseBean.accessToken, bundle!!.getString(EXTRA_CLIENT_ID))
    }

    fun setAuthorizeResponseBean(authorizeResponseBean: AuthorizeResponseBean) {
        fetchAndFinish(false, authorizeResponseBean.accessToken, authorizeResponseBean.refreshToken)
    }

    fun fetchAndFinish(changeOid: Boolean, accessToken: String, refreshToken: String) {
        val fetchLiveData = repository.getAllInfo(accessToken, refreshToken)
        oauth2LiveData.addSource(fetchLiveData, {
            it!!
            when (it.status) {
                Resource.STATUS_SUCCESS -> {
                    oauth2LiveData.removeSource(fetchLiveData)
                    oauth2LiveData.value = Resource.success(it.data)
                }
                Resource.STATUS_ERROR -> {
                    oauth2LiveData.removeSource(fetchLiveData)
                    oauth2LiveData.value = Resource.fail(it.err)
                }
                Resource.STATUS_LOADING -> {
                    oauth2LiveData.value = Resource.loading(it.loading)
                }
            }
        })
    }
}

class OAuth2Repository(val preference: Preference, val database: Database, val network: Network) {

    fun getAllInfo(token: String, refreshToken: String): LiveData<Resource<OAuth2ViewData>> {
        val userLiveData = user(token)
        val appLiveData = app(token)
        val foo = MediatorLiveData<Resource<OAuth2ViewData>>()
        val observer = { resource: Resource<Any>? ->
            resource!!
            when (resource.status) {
                Resource.STATUS_SUCCESS -> {
                    val userValue = userLiveData.value!!
                    val appValue = appLiveData.value!!
                    if (userValue.status == Resource.STATUS_SUCCESS
                            && appValue.status == Resource.STATUS_SUCCESS) {
                        foo.value = Resource.success(OAuth2ViewData(userValue.data!!.name, appValue.data!!.name, userValue.data.oid, token, refreshToken))
                    }
                }
                Resource.STATUS_ERROR -> {
                    foo.removeSource(userLiveData)
                    foo.removeSource(appLiveData)
                    foo.value = Resource.fail(resource.err)
                }
                Resource.STATUS_LOADING -> {
                    foo.value = Resource.loading(resource.loading)
                }
            }
        }
        foo.addSource(userLiveData, observer)
        foo.addSource(appLiveData, observer)
        return foo
    }

    fun user(token: String): LiveData<Resource<UserData>> {
        return object : ResourceFetcher<UserData>("network: user") {
            override fun network(): LiveData<MyResponse<UserData>> {
                return Transformations.map(network.user(token), {
                    it!!
                    it.body!!
                    return@map MyResponse(it.code, it.headers, if (it.isSuccess()) UserData(it.body.oid, it.body.name) else null, it.err)
                })
            }
        }
                .fetch()
                .asLiveData()
    }

    fun app(token: String): LiveData<Resource<AppData>> {
        return object : ResourceFetcher<AppData>("network: app") {
            override fun network(): LiveData<MyResponse<AppData>> {
                return Transformations.map(network.app(token), {
                    it!!
                    it.body!!
                    return@map MyResponse(it.code, it.headers, if (it.isSuccess()) AppData(it.body.oid, it.body.name) else null, it.err)
                })
            }
        }
                .fetch()
                .asLiveData()
    }
}

data class StartLoginInput(val requestId: Int)
data class StartTokenInput(
        val requestId: Int,
        val token: String,
        val clientId: String
)

data class UserData(val oid: String, val name: String)
data class AppData(val oid: String, val name: String)
data class TokenData(val oid: String, val accessToken: String, val refreshToken: String)

data class OAuth2ViewData(
        var username: String,
        var appName: String,
        var oid: String,
        var accessToken: String,
        var refreshToken: String
)