package ru.descend.bot.lolapi.leaguedata.match_dto

data class Style(
    val description: String,
    val selections: List<Selection>,
    val style: Int
)