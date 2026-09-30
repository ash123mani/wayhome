package com.wayhome.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wayhome.domain.model.UserProfile
import com.wayhome.domain.repository.ProfileRepository
import com.wayhome.nearby.NearbyTransport
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repo: ProfileRepository,
    private val transport: NearbyTransport
) : ViewModel() {
    val profile: StateFlow<UserProfile?> = repo.observeProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        viewModelScope.launch { repo.getOrCreateTempIdentity() }
    }

    fun setDiscoverable(looking: Boolean) {
        viewModelScope.launch {
            repo.setDiscoverable(looking)
            if (!looking) transport.stopAdvertising() else {
                val p = repo.getOrCreateTempIdentity()
                transport.startAdvertising(p.tempId, p.destination, true)
            }
        }
    }

    fun stopSharing() {
        viewModelScope.launch { repo.stopSharing() }
    }
}
