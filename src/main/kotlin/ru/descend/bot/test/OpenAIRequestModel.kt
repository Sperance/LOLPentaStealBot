package ru.descend.bot.test

import com.google.gson.annotations.SerializedName


class OpenAIRequestModel(
    @field:SerializedName("model") private val model: String, @field:SerializedName(
        "messages"
    ) private val messages: List<Message>, @field:SerializedName(
        "temperature"
    ) private val temperature: Float
)


class Message(
    @field:SerializedName("role") private val role: String, @field:SerializedName(
        "content"
    ) private val content: String
)