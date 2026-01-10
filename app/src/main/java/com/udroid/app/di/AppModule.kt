package com.udroid.app.di

import com.udroid.app.session.UbuntuSessionManager
import com.udroid.app.session.UbuntuSessionManagerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindUbuntuSessionManager(
        impl: UbuntuSessionManagerImpl
    ): UbuntuSessionManager
}
