package ru.descend.bot.lolapi.dto.match_dto

data class Team(
    val bans: List<Any>,
    val objectives: Objectives,
    val teamId: Int,
    val win: Boolean
)