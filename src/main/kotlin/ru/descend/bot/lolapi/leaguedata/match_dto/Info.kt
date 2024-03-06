package ru.descend.bot.lolapi.leaguedata.match_dto

data class Info(
    val endOfGameResult: String,
    val gameCreation: Long,
    val gameDuration: Int,
    val gameEndTimestamp: Long,
    val gameId: Int,
    val gameMode: String,
    val gameName: String,
    val gameStartTimestamp: Long,
    val gameType: String,
    val gameVersion: String,
    val mapId: Int,
    val participants: List<Participant>,
    val platformId: String,
    val queueId: Int,
    val teams: List<Team>,
    val tournamentCode: String
)