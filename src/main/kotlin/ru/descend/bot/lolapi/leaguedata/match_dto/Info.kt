package ru.descend.bot.lolapi.leaguedata.match_dto

import ru.descend.bot.firebase.FirePerson

data class Info(
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
){

    fun getCurrentParticipant(puuid: String) : Participant? {
        return participants.find { it.puuid == puuid }
    }
}