package com.wayhome.core

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.wayhome.domain.model.Destination
import com.wayhome.domain.model.Location
import com.wayhome.domain.model.UserProfile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

private val Context.dataStore by preferencesDataStore("wayhome_identity")

/** Session-scoped temporary identity. No login/signup/email/phone. */
@Singleton
class IdentityManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json
) {
    private val tempIdKey = stringPreferencesKey("temp_id")
    private val destKey = stringPreferencesKey("destination")
    private val lookingKey = stringPreferencesKey("looking")

    fun observeProfile(): Flow<UserProfile?> =
        context.dataStore.data.map { prefs ->
            val id = prefs[tempIdKey] ?: return@map null
            val dest = prefs[destKey]?.let {
                runCatching { json.decodeFromString<Destination>(it) }.getOrNull()
            } ?: Destination("Bengaluru", "Whitefield", Location(12.9698, 77.7499))
            UserProfile(id, dest, prefs[lookingKey] != "0")
        }

    suspend fun getOrCreate(): UserProfile {
        var current: UserProfile? = null
        context.dataStore.data.map {
            val id = it[tempIdKey]
            if (id != null) {
                val dest = it[destKey]?.let { d ->
                    runCatching { json.decodeFromString<Destination>(d) }.getOrNull()
                } ?: Destination("Bengaluru", "Whitefield", Location(12.9698, 77.7499))
                UserProfile(id, dest, it[lookingKey] != "0")
            } else null
        }.let {
            // collect once via edit block instead
        }
        context.dataStore.edit { prefs ->
            var id = prefs[tempIdKey]
            if (id == null) {
                id = "Traveller ${100 + Random.nextInt(900)}"
                prefs[tempIdKey] = id
            }
            val destStr = prefs[destKey]
            if (destStr == null) {
                prefs[destKey] = json.encodeToString(
                    Destination("Bengaluru", "Whitefield", Location(12.9698, 77.7499))
                )
            }
            current = UserProfile(
                id!!,
                prefs[destKey]?.let { d -> runCatching { json.decodeFromString<Destination>(d) }.getOrNull() }
                    ?: Destination("Bengaluru", "Whitefield", Location(12.9698, 77.7499)),
                prefs[lookingKey] != "0"
            )
        }
        return current!!
    }

    suspend fun updateDestination(destination: Destination) {
        context.dataStore.edit { it[destKey] = json.encodeToString(destination) }
    }

    suspend fun setLooking(looking: Boolean) {
        context.dataStore.edit { it[lookingKey] = if (looking) "1" else "0" }
    }
}
