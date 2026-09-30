package com.wayhome.di

import com.wayhome.BuildConfig
import com.wayhome.data.routing.CityRouteOriginProvider
import com.wayhome.data.routing.OsrmApi
import com.wayhome.data.routing.OsrmRouteRepository
import com.wayhome.domain.repository.RouteRepository
import com.wayhome.domain.routing.OsrmRouteMatcher
import com.wayhome.domain.routing.RouteMatchConfig
import com.wayhome.domain.routing.RouteMatcher
import com.wayhome.domain.routing.RouteOriginProvider
import com.wayhome.domain.usecase.FindRouteMatchesUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Route-based matching graph. The OSRM endpoint comes from
 * `BuildConfig.OSRM_BASE_URL` (override with `-POSRM_BASE_URL=...`), so the
 * public demo server can be swapped for a self-hosted instance without code
 * changes. Nothing here touches Nearby, Room, or the existing matcher.
 */
@Module
@InstallIn(SingletonComponent::class)
object RoutingModule {
    @Provides @Singleton @OsrmBaseUrl
    fun provideOsrmBaseUrl(): String {
        val raw = BuildConfig.OSRM_BASE_URL.trim()
        require(raw.isNotBlank()) { "OSRM_BASE_URL must not be blank" }
        return if (raw.endsWith("/")) raw else "$raw/"
    }

    @Provides @Singleton
    fun provideRouteMatchConfig(): RouteMatchConfig = RouteMatchConfig()

    @Provides @Singleton
    fun provideOsrmApi(json: Json, @OsrmBaseUrl baseUrl: String): OsrmApi {
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .callTimeout(20, TimeUnit.SECONDS)
            .build()
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(OsrmApi::class.java)
    }

    @Provides @Singleton
    fun provideRouteRepository(impl: OsrmRouteRepository): RouteRepository = impl

    @Provides @Singleton
    fun provideRouteMatcher(impl: OsrmRouteMatcher): RouteMatcher = impl

    @Provides @Singleton
    fun provideRouteOriginProvider(impl: CityRouteOriginProvider): RouteOriginProvider = impl

    @Provides @Singleton
    fun provideFindRouteMatchesUseCase(
        matcher: RouteMatcher,
        config: RouteMatchConfig
    ): FindRouteMatchesUseCase = FindRouteMatchesUseCase(matcher, config)
}
