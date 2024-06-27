package ru.descend.bot.lolapi.dto.matchDto

data class Metadata(
    val dataVersion: String,
    val matchId: String,
    val participants: List<String>
)