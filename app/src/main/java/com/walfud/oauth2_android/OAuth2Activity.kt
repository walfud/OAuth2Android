package com.walfud.oauth2_android

import android.app.Activity
import android.app.Dialog
import android.arch.lifecycle.*
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import com.walfud.oauth2_android.dagger2.DaggerOAuth2Component
import com.walfud.oauth2_android.dagger2.OAuth2Module
import com.walfud.oauth2_android.retrofit2.MyResponse
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

        if (TextUtils.isEmpty(repository.preference.oid)) {
            // Login Activity
            startLoginInput.value = StartLoginInput(OAuth2Activity.REQUEST_LOGIN)
        } else {
            if (bundle?.containsKey(EXTRA_CLIENT_ID) ?: false) {
                // Token Activity
                val token = repository.getToken()
                oauth2LiveData.addSource(token, {
                    it!!
                    when (it.status) {
                        Resource.STATUS_SUCCESS -> {
                            oauth2LiveData.removeSource(token)
                            startTokenInput.value = StartTokenInput(OAuth2Activity.REQUEST_TOKEN, it.data!!, bundle!!.getString(EXTRA_CLIENT_ID))
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
                finishWithLocalData()
            }
        }
    }

    fun setLoginResponse(loginResponseBean: LoginResponseBean) {
        if (bundle?.containsKey(EXTRA_CLIENT_ID) ?: false) {
            // Token Activity
            startTokenInput.value = StartTokenInput(OAuth2Activity.REQUEST_TOKEN, loginResponseBean.accessToken, bundle!!.getString(EXTRA_CLIENT_ID))
        } else {
            // Login Data
            finishWithLocalData()
        }
    }

    fun setTokenResponseBean(tokenResponseBean: TokenResponseBean) {
        val allInfo = repository.getAllInfo(tokenResponseBean.accessToken, tokenResponseBean.refreshToken)
        oauth2LiveData.addSource(allInfo, {
            it!!
            when (it.status) {
                Resource.STATUS_SUCCESS -> {
                    oauth2LiveData.removeSource(allInfo)
                    oauth2LiveData.value = Resource.success(it.data)
                }
                Resource.STATUS_ERROR -> {
                    oauth2LiveData.removeSource(allInfo)
                    oauth2LiveData.value = Resource.fail(it.err)
                }
                Resource.STATUS_LOADING -> {
                    oauth2LiveData.value = Resource.loading(it.loading)
                }
            }
        })
    }

    fun query(oid: String): MutableLiveData<Resource<OAuth2ViewData>> {
        val oauth2ViewDataLiveData = MutableLiveData<Resource<OAuth2ViewData>>()

        val oauth2 = repository.database.oauth2Dao().query(oid)
        val user = Transformations.switchMap(oauth2, {
            repository.database.userDao().query(oauth2.value!!.user_name!!)
        })!!
        val app = Transformations.switchMap(oauth2, {
            repository.database.appDao().query(oauth2.value!!.app_name!!)
        })!!

        val observer = { oid: String, oauth2: LiveData<OAuth2>, user: LiveData<User>, app: LiveData<App> ->
            if (user.value != null && app.value != null) {
                oauth2ViewDataLiveData.value = Resource.success(OAuth2ViewData(user.value!!.name!!, app.value!!.name!!, oid, oauth2.value!!.accessToken!!, oauth2.value!!.refreshToken!!))
            }
        }
        val foo = MediatorLiveData<OAuth2>()
        foo.addSource(user, {
            foo.removeSource(user)
            observer(oid, oauth2, user, app)
        })
        foo.addSource(app, {
            foo.removeSource(app)
            observer(oid, oauth2, user, app)
        })

        return oauth2ViewDataLiveData
    }

    fun finishWithLocalData() {
        val foo = MediatorLiveData<Resource<OAuth2ViewData>>()
        val oauth2ViewData = query(repository.preference.oid!!)
        foo.addSource(oauth2ViewData, {
            it!!
            when (it.status) {
                Resource.STATUS_SUCCESS -> {
                    foo.removeSource(oauth2ViewData)
                    it.data!!
                    oauth2LiveData.value = Resource.success(OAuth2ViewData(it.data.username, it.data.appName, it.data.oid, it.data.accessToken, it.data.refreshToken))
                }
                Resource.STATUS_ERROR -> {
                    foo.removeSource(oauth2ViewData)
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

    fun getToken(): LiveData<Resource<String>> {
        return object : ResourceFetcher<String>("database: token") {
            override fun disk(): LiveData<String> {
                return Transformations.map(database.oauth2Dao().query(preference.oid!!), { oAuth2 ->
                    oAuth2.accessToken
                })
            }
        }
                .fetch()
                .asLiveData()
    }

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
                        foo.value = Resource.success(OAuth2ViewData(userValue.data!!.name, appValue.data!!.name, userValue.data!!.oid, token, refreshToken))
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
        foo.addSource(userLiveData, {
            Log.e("aa", it.toString())
        })
        foo.addSource(appLiveData, observer)

        return foo
    }

    fun user(token: String): LiveData<Resource<UserResponseBean>> {
        return object : ResourceFetcher<UserResponseBean>("network: user") {
            override fun network(): LiveData<MyResponse<UserResponseBean>> {
                return network.user(token)
            }
        }
                .fetch()
                .asLiveData()
    }

    fun app(token: String): LiveData<Resource<AppResponseBean>> {
        return object : ResourceFetcher<AppResponseBean>("network: app") {
            override fun network(): LiveData<MyResponse<AppResponseBean>> {
                return network.app(token)
            }
        }
                .fetch()
                .asLiveData()
    }
//
//    fun saveOAuth2Data(oauth2Data: OAuth2Data): LiveData<OAuth2Data> {
//        val liveData = MutableLiveData<OAuth2Data>()
//        async(CommonPool) {
//            database.beginTransaction()
//            try {
//                database.userDao().upsert(User(oauth2Data.username))
//                database.appDao().upsert(App(oauth2Data.appName))
//                database.oauth2Dao().upsert(OAuth2(
//                        oauth2Data.username,
//                        oauth2Data.appName,
//                        oauth2Data.oid,
//                        oauth2Data.accessToken,
//                        oauth2Data.refreshToken
//                ))
//                database.setTransactionSuccessful()
//                liveData.postValue(oauth2Data)
//            } catch (e: Exception) {
//                e.printStackTrace()
//                liveData.postValue(null)
//            } finally {
//                database.endTransaction()
//            }
//        }
//        return liveData
//    }
}

data class StartLoginInput(val requestId: Int)
data class StartTokenInput(
        val requestId: Int,
        val token: String,
        val clientId: String
)

data class OAuth2ViewData(
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