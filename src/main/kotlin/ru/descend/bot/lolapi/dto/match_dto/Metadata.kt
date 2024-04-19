package ru.descend.bot.lolapi.dto.match_dto

data class Metadata(
    val dataVersion: String,
    val matchId: String,
    val participants: List<String>
)