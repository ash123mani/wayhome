package com.wayhome.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wayhome.core.IdentityManager
import com.wayhome.domain.model.Message
import com.wayhome.domain.repository.ChatRepository
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
class ChatViewModel @Inject constructor(
    private val chat: ChatRepository,
    private val identity: IdentityManager
) : ViewModel() {
    private val endpointId = MutableStateFlow("")
    private val peerTempId = MutableStateFlow("")
    private val myId = MutableStateFlow("")

    fun open(ep: String, peer: String) {
        endpointId.value = ep
        peerTempId.value = peer
        viewModelScope.launch { myId.value = identity.getOrCreate().tempId }
    }

    private fun threadId(): String {
        val ids = listOf(myId.value.ifBlank { "me" }, peerTempId.value.ifBlank { "peer" }).sorted()
        return "dm:${ids[0]}|${ids[1]}"
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val messages: StateFlow<List<Message.Text>> =
        myId.flatMapLatest { me ->
            if (me.isBlank() || peerTempId.value.isBlank()) flowOf(emptyList())
            else chat.observeDirectMessages(threadId())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun send(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            chat.sendDirect(threadId(), endpointId.value, text.trim())
        }
    }
}
