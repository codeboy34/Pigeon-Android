package com.pigeonmessenger.utils

import java.lang.reflect.Type

import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.pigeonmessenger.crypto.Base64

object GsonHelper {
    val customGson = GsonBuilder().registerTypeHierarchyAdapter(ByteArray::class.java, ByteArrayToBase64TypeAdapter()).create()

    private class ByteArrayToBase64TypeAdapter : JsonSerializer<ByteArray>, JsonDeserializer<ByteArray> {
        override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): ByteArray {
            return Base64.decode(json.asString)
        }

        override fun serialize(src: ByteArray, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
            return JsonPrimitive(Base64.encodeBytes(src))
        }
    }
}