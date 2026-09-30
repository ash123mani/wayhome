package com.wayhome.presentation.group

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wayhome.core.IdentityManager
import com.wayhome.domain.model.Message
import com.wayhome.domain.model.RideGroup
import com.wayhome.domain.repository.ChatRepository
import com.wayhome.domain.repository.DiscoveryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GroupViewModel @Inject constructor(
    private val chat: ChatRepository,
    private val identity: IdentityManager,
    discovery: DiscoveryRepository
) : ViewModel() {
    val groups: StateFlow<List<RideGroup>> = chat.observeGroups()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val peers = discovery.observePeers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _myId = MutableStateFlow("")
    val myId: StateFlow<String> = _myId

    init {
        viewModelScope.launch { _myId.value = identity.getOrCreate().tempId }
    }

    private val selected = MutableStateFlow<String?>(null)
    val selectedGroupId: StateFlow<String?> = selected

    @OptIn(ExperimentalCoroutinesApi::class)
    val messages: StateFlow<List<Message.GroupText>> = selected.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else chat.observeGroupMessages(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun select(id: String) { selected.value = id }

    fun create(name: String) {
        viewModelScope.launch {
            val memberIds = peers.value.take(5).map { it.tempId }
            val g = chat.createGroup(name.ifBlank { "Whitefield Ride Group" }, memberIds)
            selected.value = g.groupId
        }
    }

    fun send(text: String) {
        val id = selected.value ?: return
        if (text.isBlank()) return
        viewModelScope.launch { chat.sendGroupText(id, text.trim()) }
    }

    fun leave() {
        val id = selected.value ?: return
        viewModelScope.launch {
            chat.leaveGroup(id)
            selected.value = null
        }
    }
}
