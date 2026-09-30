package com.wayhome.presentation.destination

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wayhome.core.IdentityManager
import com.wayhome.data.DestinationCatalog
import com.wayhome.domain.model.Destination
import com.wayhome.domain.model.Location
import com.wayhome.nearby.NearbyTransport
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DestinationViewModel @Inject constructor(
    private val identity: IdentityManager,
    private val catalog: DestinationCatalog,
    private val transport: NearbyTransport
) : ViewModel() {
    val cities = catalog.cities
    val selectedCity = MutableStateFlow(cities.firstOrNull()?.name ?: "Bengaluru")
    val selectedArea = MutableStateFlow(cities.firstOrNull()?.areas?.firstOrNull()?.name ?: "Whitefield")
    val customCity = MutableStateFlow("")
    val customArea = MutableStateFlow("")
    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving

    fun saveAndDiscover(onDone: () -> Unit) {
        viewModelScope.launch {
            _saving.value = true
            val city = customCity.value.ifBlank { selectedCity.value }
            val area = customArea.value.ifBlank { selectedArea.value }
            val loc = cities.firstOrNull { it.name == city }
                ?.areas?.firstOrNull { it.name == area }
            val dest = Destination(city, area, Location(loc?.lat, loc?.lng))
            identity.getOrCreate()
            identity.updateDestination(dest)
            identity.setLooking(true)
            val profile = identity.getOrCreate()
            transport.startAdvertising(profile.tempId, dest, true)
            transport.startDiscovery()
            _saving.value = false
            onDone()
        }
    }
}
