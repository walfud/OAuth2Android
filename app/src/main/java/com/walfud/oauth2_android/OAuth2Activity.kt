package com.walfud.oauth2_android

import android.app.Activity
import android.app.Dialog
import android.arch.lifecycle.*
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
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
        val REQUEST_TOKEN = 2
    }

    @Inject lateinit var viewModel: OAuth2ViewModel
    lateinit var dialog: Dialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        DaggerOAuth2Component.builder()
                .applicationComponent((application as OAuth2Application).component)
                .oAuth2Module(OAuth2Module(this))
                .build()
                .inject(this)
        dialog = Dialog(this)

        viewModel.startLoginInput.observe(this, Observer {
            it!!
            LoginActivity.startActivityForResult(this, it.requestId)
        })
        viewModel.startTokenInput.observe(this, Observer {
            it!!
            TokenActivity.startActivityForResult(this, it.requestId, it.token, it.clientId)
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
                val tokenResponseBean = data.getSerializableExtra(EXTRA_TOKEN_RESPONSE_BEAN) as TokenResponseBean
                viewModel.setTokenResponseBean(tokenResponseBean)
            }
        }
    }
}

class OAuth2ViewModel : ViewModel() {
    lateinit var repository: OAuth2Repository

    val oauth2LiveData = MediatorLiveData<Resource<OAuth2ViewData>>()
    val startLoginInput = MutableLiveData<StartLoginInput>()
    val startTokenInput = MutableLiveData<StartTokenInput>()
    var bundle: Bundle? = null
    fun dispatch(bundle: Bundle?) {
        this.bundle = bundle

        if (TextUtils.isEmpty(repository.oid)) {
            // Login Activity
            startLoginInput.value = StartLoginInput(OAuth2Activity.REQUEST_LOGIN)
        } else {
            if (bundle?.containsKey(EXTRA_CLIENT_ID) ?: false) {
                // Token Activity
                val token = repository.token(repository.oid!!)
                oauth2LiveData.addSource(token, {
                    it!!
                    when (it.status) {
                        Resource.STATUS_SUCCESS -> {
                            oauth2LiveData.removeSource(token)
                            startTokenInput.value = StartTokenInput(OAuth2Activity.REQUEST_TOKEN, it.data!!.accessToken!!, bundle!!.getString(EXTRA_CLIENT_ID))
                        }
                        Resource.STATUS_ERROR -> {
                            oauth2LiveData.removeSource(token)
                            oauth2LiveData.value = Resource.fail(it.err)
                        }
                        Resource.STATUS_LOADING -> {
                            oauth2LiveData.value = Resource.loading(it.loading)
                        }
                    }
                })
            } else {
                // Login Data
                querySaveAndFinish(repository.oid!!)
            }
        }
    }

    fun setLoginResponse(loginResponseBean: LoginResponseBean) {
        if (bundle?.containsKey(EXTRA_CLIENT_ID) ?: false) {
            // Token Activity
            startTokenInput.value = StartTokenInput(OAuth2Activity.REQUEST_TOKEN, loginResponseBean.accessToken, bundle!!.getString(EXTRA_CLIENT_ID))
        } else {
            // Login Data
            fetchSaveAndFinish(true, loginResponseBean.accessToken, loginResponseBean.refreshToken)
        }
    }

    fun setTokenResponseBean(tokenResponseBean: TokenResponseBean) {
        fetchSaveAndFinish(false, tokenResponseBean.accessToken, tokenResponseBean.refreshToken)
    }

    fun fetchSaveAndFinish(changeOid: Boolean, accessToken: String, refreshToken: String) {
        val fetchLiveData = repository.getAllInfo(accessToken, refreshToken)
        oauth2LiveData.addSource(fetchLiveData, {
            it!!
            when (it.status) {
                Resource.STATUS_SUCCESS -> {
                    oauth2LiveData.removeSource(fetchLiveData)

                    val saveLiveData = repository.saveOAuth2Data(it.data!!)
                    oauth2LiveData.addSource(saveLiveData, {
                        it!!
                        when (it.status) {
                            Resource.STATUS_SUCCESS -> {
                                oauth2LiveData.removeSource(saveLiveData)
                                if (changeOid) {
                                    repository.oid = it.data!!.oid
                                }
                                oauth2LiveData.value = Resource.success(it.data)
                            }
                            Resource.STATUS_ERROR -> {
                                oauth2LiveData.removeSource(saveLiveData)
                                oauth2LiveData.value = Resource.fail(it.err)
                            }
                            Resource.STATUS_LOADING -> {
                                oauth2LiveData.value = Resource.loading(it.loading)
                            }
                        }
                    })
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

    fun querySaveAndFinish(oid: String) {
        val tokenLiveData = repository.token(oid)
        oauth2LiveData.addSource(tokenLiveData, { token ->
            token!!
            when (token.status) {
                Resource.STATUS_SUCCESS -> {
                    oauth2LiveData.removeSource(tokenLiveData)
                    token.data!!
                    val userLiveData = repository.user(token.data.accessToken)
                    val appLiveData = repository.app(token.data.accessToken)

                    val joinObserver = { resource: Resource<Any>? ->
                        resource!!
                        when (resource.status) {
                            Resource.STATUS_SUCCESS -> {
                                if (userLiveData.value?.status == Resource.STATUS_SUCCESS && appLiveData.value?.status == Resource.STATUS_SUCCESS) {
                                    oauth2LiveData.removeSource(userLiveData)
                                    oauth2LiveData.removeSource(appLiveData)
                                    oauth2LiveData.value = Resource.success(OAuth2ViewData(appLiveData.value!!.data!!.name, appLiveData.value!!.data!!.name, oid, tokenLiveData.value!!.data!!.accessToken, tokenLiveData.value!!.data!!.refreshToken))
                                }
                            }
                            Resource.STATUS_ERROR -> {
                                oauth2LiveData.removeSource(userLiveData)
                                oauth2LiveData.removeSource(appLiveData)
                            }
                            Resource.STATUS_LOADING -> {
                                oauth2LiveData.value = Resource.loading(resource.loading)
                            }
                        }
                    }
                    oauth2LiveData.addSource(userLiveData, {
                        joinObserver(it)
                    })
                    oauth2LiveData.addSource(appLiveData, {
                        joinObserver(it)
                    })
                }
                Resource.STATUS_ERROR -> {
                    oauth2LiveData.removeSource(tokenLiveData)
                    oauth2LiveData.value = Resource.fail(token.err)
                }
                Resource.STATUS_LOADING -> {
                    oauth2LiveData.value = Resource.loading(token.loading)
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

    var oid
        get() = preference.oid
        set(value) {
            preference.oid = value
        }

    fun token(oid: String): LiveData<Resource<TokenData>> {
        return object : ResourceFetcher<TokenData>("database: token") {
            override fun disk(): LiveData<TokenData> {
                return Transformations.map(database.tokenDao().query(oid), {
                    it!!
                    return@map TokenData(it.oid!!, it.accessToken!!, it.refreshToken!!)
                })
            }
        }
                .fetch()
                .asLiveData()
    }

    fun user(token: String): LiveData<Resource<UserData>> {
        return object : ResourceFetcher<UserData>("network: user") {
            override fun disk(): LiveData<UserData> {
                return Transformations.switchMap(database.tokenDao().queryByToken(token), { token ->
                    if (token == null) {
                        val empty = MutableLiveData<UserData>()
                        empty.value = null
                        return@switchMap empty
                    } else {
                        return@switchMap Transformations.map(database.userDao().query(token.userId!!), { user ->
                            user!!
                            return@map UserData(token.oid!!, user.name!!)
                        })
                    }
                })
            }

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

    fun saveOAuth2Data(oauth2Data: OAuth2ViewData): LiveData<Resource<OAuth2ViewData>> {
        val liveData = MutableLiveData<Resource<OAuth2ViewData>>()
        async(CommonPool) {
            Thread.sleep(2000)          // DEBUG
            liveData.postValue(Resource.loading("disk: save"))
            database.beginTransaction()
            try {
                val token = database.tokenDao().querySync(oauth2Data.oid)
                val userId = database.userDao().upsertSync(User(token?.userId, oauth2Data.username))
                val appId = database.appDao().upsertSync(App(token?.appId, oauth2Data.appName))
                database.tokenDao().upsertSync(Token(
                        oauth2Data.oid,
                        userId,
                        appId,
                        oauth2Data.accessToken,
                        oauth2Data.refreshToken
                ))
                database.setTransactionSuccessful()

                liveData.postValue(Resource.success(oauth2Data))
            } catch (e: Exception) {
                e.printStackTrace()
                liveData.postValue(Resource.fail(e.message))
            } finally {
                database.endTransaction()
            }
        }
        return liveData
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