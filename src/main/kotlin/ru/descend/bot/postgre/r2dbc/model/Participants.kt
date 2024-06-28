package ru.descend.bot.postgre.r2dbc.model

import org.komapper.annotation.KomapperAutoIncrement
import org.komapper.annotation.KomapperEntity
import org.komapper.annotation.KomapperId
import org.komapper.annotation.KomapperTable
import org.komapper.core.dsl.Meta
import ru.descend.bot.lolapi.dto.matchDto.Participant
import ru.descend.bot.postgre.R2DBC
import ru.descend.bot.postgre.r2dbc.model.LOLs.Companion.tbl_lols
import ru.descend.bot.to1Digits

@KomapperEntity
@KomapperTable("tbl_participants")
data class Participants(
    @KomapperId
    @KomapperAutoIncrement
    val id: Int = 0,

    var match_id: Int = -1,
    var LOLperson_id: Int = -1,
    var championId: Int = -1,
    var championName: String = "",
    var kills5: Int = 0,
    var kills4: Int = 0,
    var kills3: Int = 0,
    var kills2: Int = 0,
    var kills: Int = 0,
    var assists: Int = 0,
    var deaths: Int = 0,
    var goldEarned: Int = 0,
    var skillsCast: Int = 0,
    var totalDmgToChampions: Int = 0,
    var totalDamageShieldedOnTeammates: Int = 0,
    var totalHealsOnTeammates: Int = 0,
    var totalDamageTaken: Int = 0,
    var damageDealtToBuildings: Int = 0,
    var timeCCingOthers: Int = 0,
    var skillshotsDodged: Int = 0,
    var enemyChampionImmobilizations: Int = 0,
    var damageTakenOnTeamPercentage: Double = 0.0,
    var teamDamagePercentage: Double = 0.0,
    var damagePerMinute: Double = 0.0,
    var kda: Double = 0.0,
    var minionsKills: Int = 0,
    var inhibitorKills: Int = 0,
    var team: Int = -1,
    var profileIcon: Int = -1,
    var win: Boolean = false,

    var snowballsHit: Int = 0, //snowballs_hit
    var skillshotsHit: Int = 0, //skillshots_hit
    var soloKills: Int = 0, //solo_kills
    var survivedSingleDigitHpCount: Int = 0, //survived_single_digit_hp_count
    var magicDamageDealtToChampions: Int = 0, //magic_damage_dealt_to_champions
    var physicalDamageDealtToChampions: Int = 0, //physical_damage_dealt_to_champions
    var trueDamageDealtToChampions: Int = 0, //true_damage_dealt_to_champions
    var effectiveHealAndShielding: Double = 0.0, //effective_heal_and_shielding
    var damageSelfMitigated: Int = 0, //damage_self_mitigated
    var largestCriticalStrike: Int = 0, //largest_critical_strike
    var survivedThreeImmobilizesInFight: Int = 0, //survived_three_immobilizes_in_fight
    var totalTimeCCDealt: Int = 0, //total_time_c_c_dealt
    var tookLargeDamageSurvived: Int = 0, //took_large_damage_survived
    var longestTimeSpentLiving: Int = 0, //longest_time_spent_living
    var totalTimeSpentDead: Int = 0, //total_time_spent_dead

    var dataKey: String = "",
    var gameMatchKey: String = "",
    var gameMatchMmr: Double = 0.0,
    var visionScore: Int = 0
) {

    constructor(participant: Participant, match: Matches, LOLperson: LOLs) : this() {
        val kill5 = participant.pentaKills
        val kill4 = participant.quadraKills - kill5
        val kill3 = participant.tripleKills - kill4
        val kill2 = participant.doubleKills - kill3

        this.match_id = match.id
        this.LOLperson_id = LOLperson.id
        this.dataKey = match.matchId + "#" + LOLperson.id

        this.championId = participant.championId
        this.championName = participant.championName
        this.kills5 = kill5
        this.kills4 = kill4
        this.kills3 = kill3
        this.kills2 = kill2
        this.kills = participant.kills
        this.assists = participant.assists
        this.deaths = participant.deaths
        this.goldEarned = participant.goldEarned
        this.skillsCast = participant.challenges?.abilityUses?:0
        this.totalDmgToChampions = participant.totalDamageDealtToChampions
        this.totalHealsOnTeammates = participant.totalHealsOnTeammates
        this.totalDamageShieldedOnTeammates = participant.totalDamageShieldedOnTeammates
        this.totalDamageTaken = participant.totalDamageTaken
        this.damageDealtToBuildings = participant.damageDealtToBuildings
        this.timeCCingOthers = participant.timeCCingOthers
        this.skillshotsDodged = participant.challenges?.skillshotsDodged?:0
        this.enemyChampionImmobilizations = participant.challenges?.enemyChampionImmobilizations?:0
        this.survivedThreeImmobilizesInFight = participant.challenges?.survivedThreeImmobilizesInFight?:0
        this.tookLargeDamageSurvived = participant.challenges?.tookLargeDamageSurvived?:0
        this.effectiveHealAndShielding = participant.challenges?.effectiveHealAndShielding?.to1Digits()?:0.0
        this.damageTakenOnTeamPercentage = participant.challenges?.damageTakenOnTeamPercentage?.to1Digits()?:0.0
        this.damagePerMinute = participant.challenges?.damagePerMinute?.to1Digits()?:0.0
        this.kda = participant.challenges?.kda?.to1Digits()?:0.0
        this.teamDamagePercentage = participant.challenges?.teamDamagePercentage?.to1Digits()?:0.0
        this.snowballsHit = participant.challenges?.snowballsHit?:0
        this.skillshotsHit = participant.challenges?.skillshotsHit?:0
        this.soloKills = participant.challenges?.soloKills?:0
        this.survivedSingleDigitHpCount = participant.challenges?.survivedSingleDigitHpCount?:0
        this.magicDamageDealtToChampions = participant.magicDamageDealtToChampions
        this.physicalDamageDealtToChampions = participant.physicalDamageDealtToChampions
        this.trueDamageDealtToChampions = participant.trueDamageDealtToChampions
        this.damageSelfMitigated = participant.damageSelfMitigated
        this.largestCriticalStrike = participant.largestCriticalStrike
        this.longestTimeSpentLiving = participant.longestTimeSpentLiving
        this.totalTimeCCDealt = participant.totalTimeCCDealt
        this.totalTimeSpentDead = participant.totalTimeSpentDead
        this.minionsKills = participant.totalMinionsKilled
        this.inhibitorKills = participant.inhibitorKills
        this.profileIcon = participant.profileIcon
        this.team = participant.teamId
        this.win = participant.win
        this.visionScore = participant.visionScore
    }

    companion object {
        val tbl_participants = Meta.participants
    }

    suspend fun LOLpersonObj() = R2DBC.getLOLone({ tbl_lols.id eq LOLperson_id })

    override fun toString(): String {
        val textLastMMR = if (gameMatchMmr != 0.0) ", gameMatchMmr='$gameMatchMmr'" else ""
        val textLastMMRKey = if (gameMatchKey != "") ", gameMatchKey='$gameMatchKey'" else ""
        return "Participants(id=$id, LOL=$LOLperson_id, match_id=$match_id, championName='$championName'$textLastMMR$textLastMMRKey)"
    }
}