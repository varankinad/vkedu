package com.example.vkedu.di

import com.example.vkedu.data.remote.api.CatalogApi
import com.example.vkedu.data.remote.api.CatalogHttpApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val BASE_URL = "http://185.103.109.134/"

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient()
    }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideCatalogHttpApi(retrofit: Retrofit): CatalogHttpApi {
        return retrofit.create(CatalogHttpApi::class.java)
    }

    @Provides
    @Singleton
    fun provideCatalogApi(httpApi: CatalogHttpApi): CatalogApi {
        return object : CatalogApi {
            override suspend fun getCatalog() = httpApi.getCatalog()

            override suspend fun getAppDetail(id: String) = httpApi.getAppDetail(id)
        }
    }
}
