package ru.descend.bot.lolapi.leaguedata.match_dto

data class Metadata(
    val dataVersion: String,
    val matchId: String,
    val participants: List<String>
)