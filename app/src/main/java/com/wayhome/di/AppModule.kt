package com.wayhome.di

import android.content.Context
import androidx.room.Room
import com.wayhome.data.local.WayHomeDb
import com.wayhome.data.ChatRepositoryImpl
import com.wayhome.data.DiscoveryRepositoryImpl
import com.wayhome.data.ProfileRepositoryImpl
import com.wayhome.domain.matcher.DestinationMatcher
import com.wayhome.domain.matcher.HaversineDestinationMatcher
import com.wayhome.domain.repository.ChatRepository
import com.wayhome.domain.repository.DiscoveryRepository
import com.wayhome.domain.repository.ProfileRepository
import com.wayhome.nearby.NearbyTransport
import com.wayhome.nearby.RealNearbyTransport
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @Singleton
    fun provideJson(): Json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Provides @Singleton @ApplicationScope
    fun provideAppScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Provides @Singleton
    fun provideDb(@ApplicationContext ctx: Context): WayHomeDb =
        Room.databaseBuilder(ctx, WayHomeDb::class.java, "wayhome.db").build()

    @Provides @Singleton
    fun provideTransport(real: RealNearbyTransport): NearbyTransport = real

    @Provides @Singleton
    fun provideProfile(impl: ProfileRepositoryImpl): ProfileRepository = impl

    @Provides @Singleton
    fun provideDiscovery(impl: DiscoveryRepositoryImpl): DiscoveryRepository = impl

    @Provides @Singleton
    fun provideChat(impl: ChatRepositoryImpl): ChatRepository = impl

    @Provides @Singleton
    fun provideMatcher(impl: HaversineDestinationMatcher): DestinationMatcher = impl
}
