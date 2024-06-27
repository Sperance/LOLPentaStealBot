package ru.descend.bot.lolapi.dto.championMasteryDto

data class ChampionMasteryDtoItem(
    val championId: Int,
    val championLevel: Int,
    val championPoints: Int,
    val championPointsSinceLastLevel: Int,
    val championPointsUntilNextLevel: Int,
    val chestGranted: Boolean,
    val lastPlayTime: Long,
    val puuid: String,
    val summonerId: String,
    val tokensEarned: Int
) {
    override fun toString(): String {
        return "ChampionMasteryDtoItem(championId=$championId, championLevel=$championLevel, championPoints=$championPoints, championPointsSinceLastLevel=$championPointsSinceLastLevel, championPointsUntilNextLevel=$championPointsUntilNextLevel, chestGranted=$chestGranted, lastPlayTime=$lastPlayTime, puuid='$puuid', summonerId='$summonerId', tokensEarned=$tokensEarned)"
    }
}