package ru.descend.bot.lolapi.leaguedata.match_dto

data class Team(
    val bans: List<Any>,
    val objectives: Objectives,
    val teamId: Int,
    val win: Boolean
)