package ru.descend.bot.lolapi.leaguedata

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
    val victimDamageDealt: List<VictimDamageDealt>?,
    val victimDamageReceived: List<VictimDamageReceived>?,
    val victimId: Long?,
    val killType: String?,
    val level: Long?,
    val assistingParticipantIds: List<Long>?,
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
)

data class VictimDamageDealt(
    val basic: Boolean,
    val magicDamage: Long,
    val name: String,
    val participantId: Long,
    val physicalDamage: Long,
    val spellName: String,
    val spellSlot: Long,
    val trueDamage: Long,
    val type: String,
)

data class VictimDamageReceived(
    val basic: Boolean,
    val magicDamage: Long,
    val name: String,
    val participantId: Long,
    val physicalDamage: Long,
    val spellName: String,
    val spellSlot: Long,
    val trueDamage: Long,
    val type: String,
)

data class Participant(
    val participantId: Long,
    val puuid: String,
)
