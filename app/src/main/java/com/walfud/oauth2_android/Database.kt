package com.walfud.oauth2_android

import android.arch.lifecycle.LiveData
import android.arch.persistence.room.*


/**
 * Created by walfud on 29/05/2017.
 */

@android.arch.persistence.room.Database(entities = arrayOf(
        User::class,
        App::class,
        Token::class
), version = 1)
abstract class Database : RoomDatabase() {
    abstract fun userDao(): UsersDao
    abstract fun appDao(): AppsDao
    abstract fun tokenDao(): Tokens2Dao
}

/////////// DAO
@Dao
interface UsersDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertSync(user: User): Long

    @Query("SELECT * FROM Users WHERE id=:arg0")
    fun query(id: Long): LiveData<User>
}

@Dao
interface AppsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertSync(app: App): Long

    @Query("SELECT * FROM Apps WHERE id=:arg0")
    fun query(id: Long): LiveData<App>
}

@Dao
interface Tokens2Dao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertSync(token: Token): Long

    @Query("SELECT * FROM Tokens WHERE userId=:arg0 AND appId=:arg1")
    fun query(userId: Long, appId: Long): LiveData<Token>

    @Query("SELECT * FROM Tokens WHERE oid=:arg0")
    fun query(oid: String): LiveData<Token>

    @Query("SELECT * FROM Tokens WHERE accessToken=:arg0")
    fun queryByToken(token: String): LiveData<Token>

    @Query("SELECT * FROM Tokens WHERE oid=:arg0")
    fun querySync(oid: String): Token?
}

////////// Entity
@Entity(tableName = "Users")
data class User(
        @PrimaryKey
        var id: Long? = null,
        var name: String? = null
)

@Entity(tableName = "Apps")
data class App(
        @PrimaryKey
        var id: Long? = null,
        var name: String? = null
)

@Entity(tableName = "Tokens",
        foreignKeys = arrayOf(
                ForeignKey(entity = User::class, parentColumns = arrayOf("id"), childColumns = arrayOf("userId")),
                ForeignKey(entity = App::class, parentColumns = arrayOf("id"), childColumns = arrayOf("appId"))
        ))
data class Token(
        @PrimaryKey
        var oid: String? = null,
        var userId: Long? = null,
        var appId: Long? = null,

        var accessToken: String? = null,
        var refreshToken: String? = null
)
