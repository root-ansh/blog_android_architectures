package work.curioustools.ae_single.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import work.curioustools.ae_single.network.UserRepo
import work.curioustools.ae_single.network.UserRepoRemoteImpl

@Module
@InstallIn(SingletonComponent::class)
abstract class RepoBinderModule {
    @Binds
    abstract fun bindsUserRepoRemote(userRemoteImpl: UserRepoRemoteImpl): UserRepo
}