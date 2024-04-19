package ru.descend.bot.lolapi.dto

data class MatchTimelineDTO(
    val metadata: Metadata,
    val info: Info,
)

data class Metadata(
    val dataVersion: String,
    val matchId: String,
    val participants: List<String>,
)

data class Info(
    val endOfGameResult: String,
    val frameInterval: Long,
    val frames: List<Frame>,
    val gameId: Long,
    val participants: List<Participant>,
)

data class Frame(
    val events: List<Event>,
    val timestamp: Long,
)

data class Event(
    val levelUpType: String?,
    val participantId: Long?,
    val skillSlot: Long?,
    val timestamp: Long,
    val type: String,
    val itemId: Long?,
    val creatorId: Long?,
    val wardType: String?,
    val realTimestamp: Long?,
    val bounty: Long?,
    val killStreakLength: Long?,
    val killerId: Long?,
    val shutdownBounty: Long?,
    val victimId: Long?,
    val killType: String?,
    val level: Long?,
    val multiKillLength: Long?,
    val killerTeamId: Long?,
    val monsterSubType: String?,
    val monsterType: String?,
    val afterId: Long?,
    val beforeId: Long?,
    val goldGain: Long?,
    val laneType: String?,
    val teamId: Long?,
    val buildingType: String?,
    val towerType: String?,
    val actualStartTime: Long?,
    val gameId: Long?,
    val winningTeam: Long?,
){
    override fun toString(): String {
        return "Event(levelUpType=$levelUpType, participantId=$participantId, skillSlot=$skillSlot, timestamp=$timestamp, type='$type', itemId=$itemId, creatorId=$creatorId, wardType=$wardType, realTimestamp=$realTimestamp, bounty=$bounty, killStreakLength=$killStreakLength, killerId=$killerId, shutdownBounty=$shutdownBounty, victimId=$victimId, killType=$killType, level=$level, multiKillLength=$multiKillLength, killerTeamId=$killerTeamId, monsterSubType=$monsterSubType, monsterType=$monsterType, afterId=$afterId, beforeId=$beforeId, goldGain=$goldGain, laneType=$laneType, teamId=$teamId, buildingType=$buildingType, towerType=$towerType, actualStartTime=$actualStartTime, gameId=$gameId, winningTeam=$winningTeam)"
    }
}

data class Participant(
    val participantId: Long,
    val puuid: String,
)
