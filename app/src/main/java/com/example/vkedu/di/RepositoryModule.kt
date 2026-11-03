package com.example.vkedu.di

import com.example.vkedu.data.repository.AppRepositoryImpl
import com.example.vkedu.domain.repository.AppRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindAppRepository(impl: AppRepositoryImpl): AppRepository
}