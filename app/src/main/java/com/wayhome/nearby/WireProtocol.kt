package com.wayhome.nearby

import com.wayhome.domain.model.Message
import kotlinx.serialization.KSerializer
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * JSON wire codec. Payloads are small UTF-8 JSON blobs sent as
 * Nearby PAYLOAD_BYTES. Chunking for >32k payloads is out of MVP scope
 * (chat/group messages are tiny).
 */
@Singleton
class WireProtocol @Inject constructor() {
    val json: Json = Json {
        classDiscriminator = "type"
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(message: Message): ByteArray =
        json.encodeToString(PolymorphicSerializer(Message::class), message).toByteArray(Charsets.UTF_8)

    fun decode(bytes: ByteArray): Message? = runCatching {
        json.decodeFromString(
            PolymorphicSerializer(Message::class),
            bytes.toString(Charsets.UTF_8)
        )
    }.getOrNull()
}
