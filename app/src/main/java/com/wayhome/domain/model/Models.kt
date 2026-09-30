package com.wayhome.domain.model

import kotlinx.serialization.Serializable

/** Approximate geo point. Never precise home address. */
@Serializable
data class Location(
    val lat: Double? = null,
    val lng: Double? = null
)

@Serializable
data class Destination(
    val city: String = "",
    val area: String = "",
    val location: Location = Location()
) {
    fun label(): String = if (city.isBlank()) area else "$city · $area"
}

data class UserProfile(
    val tempId: String,
    val destination: Destination,
    val lookingForPartners: Boolean = true
)

data class Peer(
    val endpointId: String,
    val tempId: String,
    val destination: Destination,
    val lookingForPartners: Boolean = true,
    val connectionState: PeerConnectionState = PeerConnectionState.FOUND
)

enum class PeerConnectionState { FOUND, CONNECTING, CONNECTED, LOST }

enum class MatchLevel { SAME_AREA, NEARBY_AREA, SAME_CITY, DIFFERENT }

data class MatchResult(
    val level: MatchLevel,
    val distanceKm: Double? = null
) {
    val goingSameWay: Boolean get() = level == MatchLevel.SAME_AREA || level == MatchLevel.NEARBY_AREA
}

data class RideGroup(
    val groupId: String,
    val name: String,
    val creatorId: String,
    val members: List<String> = emptyList()
)
