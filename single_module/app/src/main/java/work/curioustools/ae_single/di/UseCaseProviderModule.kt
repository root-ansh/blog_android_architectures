package work.curioustools.ae_single.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import work.curioustools.ae_single.network.UserRepo
import work.curioustools.ae_single.network.CreateUserUseCase
import work.curioustools.ae_single.network.GetAllUsersUseCase
import work.curioustools.ae_single.network.GetSingleUserUseCase
import work.curioustools.ae_single.network.UpdateUserUseCase


@Module
@InstallIn(SingletonComponent::class)
class UseCaseProviderModule {

    @Provides
    fun provideGetAllUsersUseCase(repo: UserRepo) = GetAllUsersUseCase(repo)

    @Provides
    fun provideGetSingleUserUseCase(repo: UserRepo) = GetSingleUserUseCase()

    @Provides
    fun provideCreateUserUseCase(repo: UserRepo) = CreateUserUseCase(repo)

    @Provides
    fun provideUpdateUserUseCase(repo: UserRepo) = UpdateUserUseCase(repo)


}