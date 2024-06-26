package ru.descend.bot.lolapi.dto.currentGameInfo

data class CurrentGameInfo(
    val bannedChampions: List<BannedChampion>,
    val gameId: Int,
    val gameLength: Int,
    val gameMode: String,
    val gameQueueConfigId: Int,
    val gameStartTime: Long,
    val gameType: String,
    val mapId: Int,
    val observers: Observers,
    val participants: List<Participant>,
    val platformId: String
)