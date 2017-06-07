package com.walfud.oauth2_android.dagger2

import android.arch.lifecycle.ViewModelProviders
import com.walfud.oauth2_android.*
import dagger.Component
import dagger.Module
import dagger.Provides

/**
 * Created by walfud on 06/06/2017.
 */

@Component(dependencies = arrayOf(ApplicationComponent::class), modules = arrayOf(AuthorizeModule::class))
@Activity
interface AuthorizeComponent {
    fun inject(activity: AuthorizeActivity)
}

@Module
class AuthorizeModule(val activity: AuthorizeActivity) {
    @Provides @Activity fun provideActivity() = activity

    @Provides @Activity fun provideViewModel(activity: AuthorizeActivity, repository: AuthorizeRepository): AuthorizeViewModel {
        val viewModel = ViewModelProviders.of(activity).get(AuthorizeViewModel::class.java)
        viewModel.repository = repository
        return viewModel
    }

    @Provides @Activity fun provideRepository(preference: Preference, database: Database, network: Network) = AuthorizeRepository(preference, database, network)
}