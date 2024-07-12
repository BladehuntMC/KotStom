package net.bladehunt.kotstom.serialization.adventure.internal

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.internal.NamedValueDecoder
import kotlinx.serialization.modules.SerializersModule
import net.kyori.adventure.nbt.BinaryTag

@InternalSerializationApi
internal abstract class TaggedAdventureDecoder : AdventureDecoder, NamedValueDecoder() {
    override val serializersModule: SerializersModule
        get() = adventureNbt.serializersModule

    abstract fun decodeTaggedBinaryTag(tag: String): BinaryTag

    final override fun decodeBinaryTag(): BinaryTag = decodeTaggedBinaryTag(popTag())
}