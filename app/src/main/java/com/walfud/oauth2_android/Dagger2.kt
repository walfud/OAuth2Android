package com.walfud.oauth2_android

import android.arch.lifecycle.ViewModelProviders
import android.arch.persistence.room.Room
import android.content.Context
import com.walfud.oauth2_android.oauth2.OAuth2Activity
import com.walfud.oauth2_android.oauth2.OAuth2Repository
import com.walfud.oauth2_android.oauth2.OAuth2ViewModel
import dagger.Component
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.lang.annotation.Documented
import javax.inject.Scope

/**
 * Created by walfud on 2017/7/25.
 */

@Scope
@Documented
@Retention(AnnotationRetention.RUNTIME)
annotation class Application

@Scope
@Documented
@Retention(AnnotationRetention.RUNTIME)
annotation class Activity

@Scope
@Documented
@Retention(AnnotationRetention.RUNTIME)
annotation class Fragment

@Component(modules = arrayOf(ApplicationModule::class))
@Application
interface ApplicationComponent {
    fun inject(oauth2Application: OAuth2Application)

    fun providePreferences(): Preference
    fun provideDatabase(): Database
    fun provideNetwork(): Network
}

@Module
class ApplicationModule(val oauth2Application: OAuth2Application) {
    @Provides @Application fun provideApplication(): OAuth2Application = oauth2Application

    @Provides @Application fun provideContext(): Context = oauth2Application.applicationContext

    @Provides @Application fun providePreferences(context: Context): Preference {
        return Preference(context)
    }

    @Provides @Application fun provideDatabase(context: Context): Database = Room.databaseBuilder(context, Database::class.java, "oauth2").build()

    @Provides @Application fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
            .addInterceptor {
                it.proceed(it.request().newBuilder()
                        .build())
            }
            .build()

    @Provides @Application fun provideNetwork(okHttpClient: OkHttpClient): Network = Retrofit.Builder()
            .baseUrl("http://oauth2.walfud.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(LiveDataCallAdapterFactory())
            .client(okHttpClient)
            .build()
            .create(Network::class.java)
}

@Component(dependencies = arrayOf(ApplicationComponent::class), modules = arrayOf(OAuth2Module::class))
@Activity
interface ActivityComponent {
    fun inject(activity: OAuth2Activity)
}

@Module
class OAuth2Module(val activity: OAuth2Activity) {
    @Provides @Activity fun provideActivity() = activity

    @Provides @Activity fun provideViewModel(activity: OAuth2Activity, repository: OAuth2Repository): OAuth2ViewModel {
        val viewModel = ViewModelProviders.of(activity).get(OAuth2ViewModel::class.java)
        viewModel.repository = repository
        return viewModel
    }

    @Provides @Activity fun provideRepository(preference: Preference, database: Database, network: Network) = OAuth2Repository(preference, database, network)
}