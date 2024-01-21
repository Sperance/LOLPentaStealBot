package ru.descend.bot.lolapi.leaguedata.currentGameInfo

data class Perks(
    val perkIds: List<Int>,
    val perkStyle: Int,
    val perkSubStyle: Int
)