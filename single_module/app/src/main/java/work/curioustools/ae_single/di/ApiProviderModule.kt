package work.curioustools.ae_single.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import work.curioustools.ae_single.network.UserAPI


@Module
@InstallIn(SingletonComponent::class)
class ApiProviderModule {

    // ---- API INJECTION
    @Provides
    fun provideUserApi(retrofit: Retrofit): UserAPI {
        return retrofit.create(UserAPI::class.java)
    }
}