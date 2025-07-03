package ru.descend.kotlintelegrambot.network.serialization.adapter

import ru.descend.kotlintelegrambot.entities.inputmedia.InputMedia
import ru.descend.kotlintelegrambot.entities.inputmedia.InputMediaAnimation
import ru.descend.kotlintelegrambot.entities.inputmedia.InputMediaAudio
import ru.descend.kotlintelegrambot.entities.inputmedia.InputMediaDocument
import ru.descend.kotlintelegrambot.entities.inputmedia.InputMediaFields
import ru.descend.kotlintelegrambot.entities.inputmedia.InputMediaPhoto
import ru.descend.kotlintelegrambot.entities.inputmedia.InputMediaVideo
import com.google.gson.JsonElement
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.lang.reflect.Type

internal class InputMediaAdapter : JsonSerializer<InputMedia> {
    override fun serialize(src: InputMedia, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        val jsonElement = when (src) {
            is InputMediaPhoto -> context.serialize(src, InputMediaPhoto::class.java)
            is InputMediaVideo -> context.serialize(src, InputMediaVideo::class.java)
            is InputMediaAnimation -> context.serialize(src, InputMediaAnimation::class.java)
            is InputMediaAudio -> context.serialize(src, InputMediaAudio::class.java)
            is InputMediaDocument -> context.serialize(src, InputMediaDocument::class.java)
        }
        val jsonObject = jsonElement.asJsonObject
        jsonObject.addProperty(InputMediaFields.TYPE, src.type)
        return jsonObject
    }
}
