package ru.descend.bot.postgre.openapi

data class RootAI(
    val choices: List<Choice2>,
    val created: Long,
    val id: String,
    val model: String,
    val `object`: String,
    val usage: Usage2,
)

data class Choice2(
    val finish_reason: String,
    val index: Long,
    val message: Message2,
    val logprobs: Any?,
)

data class Message2(
    val content: String,
    val role: String,
)

data class Usage2(
    val completion_tokens: Long,
    val prompt_tokens: Long,
    val total_tokens: Long,
)