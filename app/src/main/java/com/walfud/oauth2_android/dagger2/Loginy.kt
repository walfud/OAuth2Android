package com.walfud.oauth2_android.dagger2

import android.arch.lifecycle.ViewModelProviders
import com.walfud.oauth2_android.*
import dagger.Component
import dagger.Module
import dagger.Provides

/**
 * Created by walfud on 06/06/2017.
 */

@Component(dependencies = arrayOf(ApplicationComponent::class), modules = arrayOf(LoginModule::class))
@Activity
interface LoginComponent {
    fun inject(activity: LoginActivity)
}

@Module
class LoginModule(val activity: LoginActivity) {
    @Provides @Activity fun provideActivity() = activity

    @Provides @Activity fun provideViewModel(activity: LoginActivity, repository: LoginRepository): LoginViewModel {
        val viewModel = ViewModelProviders.of(activity).get(LoginViewModel::class.java)
        viewModel.repository = repository
        return viewModel
    }

    @Provides @Activity fun provideRepository(preference: Preference, database: Database, network: Network) = LoginRepository(preference, database, network)
}