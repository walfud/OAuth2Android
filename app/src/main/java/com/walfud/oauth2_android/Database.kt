package com.walfud.oauth2_android

import android.arch.lifecycle.LiveData
import android.arch.persistence.room.*


/**
 * Created by walfud on 29/05/2017.
 */

val database by lazy { Room.databaseBuilder(context, Database::class.java, "oauth2").build()!! }

@android.arch.persistence.room.Database(entities = arrayOf(
        User::class,
        App::class,
        OAuth2::class
), version = 1)
abstract class Database : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun appDao(): AppDao
    abstract fun oauth2Dao(): OAuth2Dao
}

/////////// DAO
@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(user: User)

    @Query("SELECT * FROM user WHERE name=:arg0")
    fun query(username: String): LiveData<User>
}

@Dao
interface AppDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(app: App)

    @Query("SELECT * FROM app WHERE name=:arg0")
    fun query(appName: String): LiveData<App>
}

@Dao
interface OAuth2Dao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(oauth2: OAuth2)

    @Query("SELECT * FROM oauth2 WHERE user_name=:arg0 AND app_name=:arg1")
    fun query(username: String, appName: String): LiveData<OAuth2>

    @Query("SELECT * FROM oauth2 WHERE oid=:arg0")
    fun query(oid: String): LiveData<OAuth2>

    @Query("SELECT * FROM oauth2 WHERE oid=:arg0")
    fun querySync(oid: String): OAuth2
}

////////// Entity
@Entity
data class User(
        @PrimaryKey
        var name: String? = null
)

@Entity
data class App(
    @PrimaryKey
    var name: String? = null
)

@Entity(foreignKeys = arrayOf(
        ForeignKey(entity = User::class, parentColumns = arrayOf("name"), childColumns = arrayOf("user_name")),
        ForeignKey(entity = App::class, parentColumns = arrayOf("name"), childColumns = arrayOf("app_name"))
))
data class OAuth2(
    var user_name: String? = null,
    var app_name: String? = null,

    @PrimaryKey
    var oid: String? = null,
    var accessToken: String? = null,
    var refreshToken: String? = null
)
