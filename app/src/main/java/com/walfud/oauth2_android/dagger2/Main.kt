package com.walfud.oauth2_android.dagger2

import com.walfud.oauth2_android.MainActivity
import dagger.Component
import dagger.Module
import dagger.Provides

/**
 * Created by walfud on 06/06/2017.
 */

@Component(dependencies = arrayOf(ApplicationComponent::class), modules = arrayOf(MainModule::class))
@Activity
interface MainComponent {
    fun inject(activity: MainActivity)
}

@Module
class MainModule(val activity: MainActivity) {
    @Provides @Activity fun provideActivity() = activity
}