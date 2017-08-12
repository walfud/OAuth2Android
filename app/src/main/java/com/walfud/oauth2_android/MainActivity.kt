package com.walfud.oauth2_android

import android.app.Activity
import android.arch.lifecycle.LifecycleActivity
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.arch.persistence.room.Room
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import com.walfud.oauth2_android.oauth2.*
import org.jetbrains.anko.*
import org.jetbrains.anko.sdk25.coroutines.onClick

/**
 * Created by walfud on 30/05/2017.
 */

val REQUEST_TEST_LOGIN = 8
val REQUEST_TEST_TOKEN = 9
val REQUEST_TEST_OAUTH2 = 10

class MainActivity : LifecycleActivity() {

    val show = MutableLiveData<ShowData>()
    lateinit var usernameTv: TextView
    lateinit var appNameTv: TextView
    lateinit var oidTv: TextView
    lateinit var accessTokenTv: TextView
    lateinit var refreshTokenTv: TextView

    val preference by lazy { com.walfud.oauth2_android.Preference(this) }
    val database by lazy { Room.databaseBuilder(this, Database::class.java, "oauth2").build() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.AppTheme)
        MainActivityUI().setContentView(this)

        show.observe(this, Observer {
            it!!
            usernameTv.text = it.username
            appNameTv.text = it.appName
            oidTv.text = it.oid
            accessTokenTv.text = it.accessToken
            refreshTokenTv.text = it.refreshToken
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode != Activity.RESULT_OK) {
            toast(data?.getStringExtra(EXTRA_ERROR) ?: "User Cancel")
            return
        }

        data!!
        when (requestCode) {
            REQUEST_TEST_LOGIN -> {
//                val loginResponseBean = data.getSerializableExtra(EXTRA_LOGIN_RESPONSE_BEAN) as LoginResponseBean
//                network.user(loginResponseBean.accessToken).observe(this, Observer { userResponse ->
//                    userResponse!!
//                    if (userResponse.isSuccess()) {
//                        userResponse.body!!
//                        network.user(loginResponseBean.accessToken).observe(this, Observer { appResponse ->
//                            appResponse!!
//                            if (appResponse.isSuccess()) {
//                                appResponse.body!!
//                                save(userResponse.body.oid, userResponse.body.name, appResponse.body.name, loginResponseBean.accessToken, loginResponseBean.refreshToken)
//                                preference.oid = userResponse.body.oid
//                                show.postValue(ShowData(userResponse.body.oid, userResponse.body.name, appResponse.body.name, loginResponseBean.accessToken, loginResponseBean.refreshToken))
//                            }
//                        })
//                    }
//                })
            }
            REQUEST_TEST_OAUTH2 -> {
                save(
                        data.getStringExtra(EXTRA_OID),
                        data.getStringExtra(EXTRA_USERNAME),
                        data.getStringExtra(EXTRA_APPNAME),
                        data.getStringExtra(EXTRA_ACCESS_TOKEN),
                        data.getStringExtra(EXTRA_REFRESH_TOKEN)
                )
                show.value = ShowData(
                        data.getStringExtra(EXTRA_OID),
                        data.getStringExtra(EXTRA_USERNAME),
                        data.getStringExtra(EXTRA_APPNAME),
                        data.getStringExtra(EXTRA_ACCESS_TOKEN),
                        data.getStringExtra(EXTRA_REFRESH_TOKEN)
                )
            }
        }
    }

    fun save(oid: String, username: String, appName: String, accessToken: String, refreshToken: String) {
//        async(CommonPool) {
//            database.beginTransaction()
//            try {
//                val token = database.tokenDao().querySync(oid)
//                val userId = database.userDao().upsertSync(User(token?.userId, username))
//                val appId = database.appDao().upsertSync(App(token?.appId, appName))
//                database.tokenDao().upsertSync(Token(
//                        oid,
//                        userId,
//                        appId,
//                        accessToken,
//                        refreshToken
//                ))
//                database.setTransactionSuccessful()
//            } catch (e: Exception) {
//                e.printStackTrace()
//            } finally {
//                database.endTransaction()
//            }
//        }
    }
}

class MainActivityUI : AnkoComponent<MainActivity> {
    override fun createView(ui: AnkoContext<MainActivity>) = with(ui) {
        verticalLayout {
            button("Test Logout") {
                onClick {
                    owner.preference.oid = null
                    toast("Logout Success")
                }
            }
            button("Test Login") {
                onClick {
                    LoginActivity.startActivityForResult(owner, REQUEST_TEST_LOGIN)
                }
            }
            button("Test Token") {
                onClick {
                    if (owner.preference.oid == null) {
                        toast("Please Login First")
                    } else {
                        owner.database.tokenDao().query(owner.preference.oid!!).observe(owner, Observer { token ->
                            token!!
                            AuthorizeActivity.startActivityForResult(owner, REQUEST_TEST_TOKEN, token.accessToken!!, "contactsync")
                        })
                    }
                }
            }
            button("Test OAuth2 Login & Token") {
                onClick {
                    startOAuth2ActivityForResult(owner, REQUEST_TEST_OAUTH2, "contactsync")
                }
            }

            owner.usernameTv = textView()
            owner.appNameTv = textView()
            owner.oidTv = textView()
            owner.accessTokenTv = textView()
            owner.refreshTokenTv = textView()
        }
    }
}

data class ShowData(
        var oid: String,
        var username: String,
        var appName: String,
        var accessToken: String,
        var refreshToken: String
)