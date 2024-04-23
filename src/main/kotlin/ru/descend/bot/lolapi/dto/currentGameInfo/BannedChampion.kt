package ru.descend.bot.lolapi.dto.currentGameInfo

data class BannedChampion(
    val championId: Int,
    val pickTurn: Int,
    val teamId: Int
)