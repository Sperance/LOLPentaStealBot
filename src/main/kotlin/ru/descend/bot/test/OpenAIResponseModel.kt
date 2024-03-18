package ru.descend.bot.test

import com.google.gson.annotations.SerializedName


class OpenAIResponseModel {
    @SerializedName("id")
    val id: String? = null

    @SerializedName("object")
    val objectType: String? = null

    @SerializedName("created")
    val createdTimestamp: Long = 0

    @SerializedName("model")
    val modelVersion: String? = null

    @SerializedName("choices")
    val choices: Array<OpenAIChoice>? = null

    @SerializedName("usage")
    val usageInfo: Usage? = null
}


class OpenAIChoice {
    @SerializedName("message")
    val message: ResponseMessage? = null

    @SerializedName("finish_reason")
    val finishReason: String? = null
}


class ResponseMessage {
    @SerializedName("role")
    val role: String? = null

    @SerializedName("content")
    val content: String? = null
}


class Usage {
    @SerializedName("prompt_tokens")
    val promptTokens = 0

    @SerializedName("completion_tokens")
    val completionTokens = 0

    @SerializedName("total_tokens")
    val totalTokens = 0
}