package ru.descend.bot.lolapi.dto.currentGameInfo

data class Participant(
    val bot: Boolean,
    val championId: Int,
    val gameCustomizationObjects: List<Any>,
    val perks: Perks,
    val profileIconId: Int,
    val puuid: String,
    val spell1Id: Int,
    val spell2Id: Int,
    val summonerId: String,
    val summonerName: String,
    val teamId: Int
)