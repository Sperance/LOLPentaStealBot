package ru.descend.bot.lolapi.leaguedata

data class SummonerDTO(
    val accountId: String,
    val id: String,
    val name: String,
    val profileIconId: Int,
    val puuid: String,
    val revisionDate: Long,
    val summonerLevel: Int
)