package com.walfud.oauth2_android.dagger2

import android.arch.lifecycle.ViewModelProviders
import com.walfud.oauth2_android.*
import dagger.Component
import dagger.Module
import dagger.Provides

/**
 * Created by walfud on 06/06/2017.
 */

@Component(dependencies = arrayOf(ApplicationComponent::class), modules = arrayOf(TokenModule::class))
@Activity
interface TokenComponent {
    fun inject(activity: TokenActivity)
}

@Module
class TokenModule(val activity: TokenActivity) {
    @Provides @Activity fun provideActivity() = activity

    @Provides @Activity fun provideViewModel(activity: TokenActivity, repository: TokenRepository): TokenViewModel {
        val viewModel = ViewModelProviders.of(activity).get(TokenViewModel::class.java)
        viewModel.repository = repository
        return viewModel
    }

    @Provides @Activity fun provideRepository(preference: Preference, database: Database, network: Network) = TokenRepository(preference, database, network)
}