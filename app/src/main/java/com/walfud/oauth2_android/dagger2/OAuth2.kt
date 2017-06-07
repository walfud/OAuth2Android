package com.walfud.oauth2_android.dagger2

import android.arch.lifecycle.ViewModelProviders
import com.walfud.oauth2_android.*
import dagger.Component
import dagger.Module
import dagger.Provides

/**
 * Created by walfud on 06/06/2017.
 */

@Component(dependencies = arrayOf(ApplicationComponent::class), modules = arrayOf(OAuth2Module::class))
@Activity
interface OAuth2Component {
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