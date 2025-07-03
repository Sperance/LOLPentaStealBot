package ru.descend.kotlintelegrambot.network.serialization.adapter

import ru.descend.kotlintelegrambot.entities.dice.DiceEmoji
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type

internal class DiceEmojiAdapter : JsonDeserializer<DiceEmoji> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext?): DiceEmoji =
        DiceEmoji.fromString(json.asString)
}
