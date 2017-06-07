package com.walfud.oauth2_android.dagger2

import android.arch.persistence.room.Room
import android.content.Context
import com.walfud.oauth2_android.Database
import com.walfud.oauth2_android.Network
import com.walfud.oauth2_android.OAuth2Application
import com.walfud.oauth2_android.Preference
import com.walfud.oauth2_android.retrofit2.LiveDataCallAdapterFactory
import dagger.Component
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Created by walfud on 06/06/2017.
 */

@Component(modules = arrayOf(ApplicationModule::class))
@Application
interface ApplicationComponent {
    fun inject(oAuth2Application: OAuth2Application)

    fun application(): OAuth2Application
    fun context(): Context
    fun preference(): Preference
    fun database(): Database
    fun network(): Network
}

@Module
class ApplicationModule(val application: OAuth2Application) {

    @Provides @Application fun provideApplication() = this.application
    @Provides @Application fun provideContext() = application.applicationContext!!

    @Provides @Application fun providePreference(context: Context): Preference {
        return Preference(context)
    }

    @Provides @Application fun provideDatabase(context: Context): Database {
        return Room.databaseBuilder(context, Database::class.java, "oauth2").build()
    }

    @Provides @Application fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
                .addInterceptor {
                    it.proceed(it.request().newBuilder()
                            .build())
                }
                .build()
    }

    @Provides @Application fun provideNetwork(okHttpClient: OkHttpClient): Network {
        return Retrofit.Builder()
                .baseUrl("http://oauth2.walfud.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(LiveDataCallAdapterFactory())
                .client(okHttpClient)
                .build()
                .create(Network::class.java)
    }
}