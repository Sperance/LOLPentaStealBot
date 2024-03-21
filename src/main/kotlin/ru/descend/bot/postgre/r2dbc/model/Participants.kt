package ru.descend.bot.postgre.r2dbc.model

import org.komapper.annotation.KomapperAutoIncrement
import org.komapper.annotation.KomapperCreatedAt
import org.komapper.annotation.KomapperEntity
import org.komapper.annotation.KomapperId
import org.komapper.annotation.KomapperTable
import org.komapper.annotation.KomapperUpdatedAt
import org.komapper.core.dsl.Meta
import org.komapper.core.dsl.QueryDsl
import ru.descend.bot.lolapi.leaguedata.match_dto.Participant
import ru.descend.bot.postgre.r2dbc.R2DBC
import ru.descend.bot.postgre.r2dbc.interfaces.InterfaceR2DBC
import ru.descend.bot.printLog
import ru.descend.bot.savedObj.calculateUpdate
import ru.descend.bot.to2Digits
import java.time.LocalDateTime

val tbl_participants = Meta.participants

@KomapperEntity
@KomapperTable("tbl_participants")
data class Participants(
    @KomapperId
    @KomapperAutoIncrement
    var id: Int = 0,

    var match_id: Int = -1,
    var LOLperson_id: Int = -1,
    var guild_id: Int = -1,

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
    var mmr: Double = 0.0,
    var mmrFlat: Double = 0.0,
    var minionsKills: Int = 0,
    var inhibitorKills: Int = 0,
    var team: Int = -1,
    var profileIcon: Int = -1,
    var win: Boolean = false,

    var snowballsHit: Int = 0, //snowballs_hit
    var skillshotsHit: Int = 0, //skillshots_hit
    var summonerLevel: Int = 0, //summoner_level
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

    @KomapperCreatedAt
    var createdAt: LocalDateTime = LocalDateTime.MIN,
    @KomapperUpdatedAt
    var updatedAt: LocalDateTime = LocalDateTime.MIN
) : InterfaceR2DBC<Participants> {

    constructor(participant: Participant, match: Matches, LOLperson: LOLs) : this() {
        val kill5 = participant.pentaKills
        val kill4 = participant.quadraKills - kill5
        val kill3 = participant.tripleKills - kill4
        val kill2 = participant.doubleKills - kill3

        this.match_id = match.id
        this.LOLperson_id = LOLperson.id
        this.guild_id = match.guild_id

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
        this.skillsCast = if (participant.challenges != null) participant.challenges.abilityUses else 0
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
        this.effectiveHealAndShielding = participant.challenges?.effectiveHealAndShielding?:0.0
        this.damageTakenOnTeamPercentage = participant.challenges?.damageTakenOnTeamPercentage?.to2Digits()?:0.0
        this.damagePerMinute = participant.challenges?.damagePerMinute?.to2Digits()?:0.0
        this.kda = participant.challenges?.kda?.to2Digits()?:0.0
        this.teamDamagePercentage = participant.challenges?.teamDamagePercentage?.to2Digits()?:0.0
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
        this.summonerLevel = participant.summonerLevel
        this.profileIcon = participant.profileIcon
        this.team = participant.teamId
        this.win = participant.win
    }

    override suspend fun save() : Participants {
        val result = R2DBC.db.withTransaction {
            R2DBC.db.runQuery { QueryDsl.insert(tbl_participants).single(this@Participants) }
        }
        this.id = result.id
        this.updatedAt = result.updatedAt
        this.createdAt = result.createdAt
        printLog("[Participants::save] $this")
        return this
    }

    override suspend fun update() : Participants {
        val before = R2DBC.getParticipants { tbl_participants.id eq this@Participants.id }.firstOrNull()
        printLog("[Participants::update] $this { ${calculateUpdate(before, this)} }")
        return R2DBC.db.withTransaction {
            R2DBC.db.runQuery { QueryDsl.update(tbl_participants).single(this@Participants) }
        }
    }

    override suspend fun delete() {
        printLog("[Participants::delete] $this")
        R2DBC.db.withTransaction {
            R2DBC.db.runQuery { QueryDsl.delete(tbl_participants).single(this@Participants) }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Participants

        if (id != other.id) return false
        if (championId != other.championId) return false
        if (championName != other.championName) return false
        if (kills5 != other.kills5) return false
        if (kills4 != other.kills4) return false
        if (kills3 != other.kills3) return false
        if (kills2 != other.kills2) return false
        if (kills != other.kills) return false
        if (assists != other.assists) return false
        if (deaths != other.deaths) return false
        if (goldEarned != other.goldEarned) return false
        if (skillsCast != other.skillsCast) return false
        if (totalDmgToChampions != other.totalDmgToChampions) return false
        if (totalDamageShieldedOnTeammates != other.totalDamageShieldedOnTeammates) return false
        if (totalHealsOnTeammates != other.totalHealsOnTeammates) return false
        if (totalDamageTaken != other.totalDamageTaken) return false
        if (damageDealtToBuildings != other.damageDealtToBuildings) return false
        if (timeCCingOthers != other.timeCCingOthers) return false
        if (skillshotsDodged != other.skillshotsDodged) return false
        if (enemyChampionImmobilizations != other.enemyChampionImmobilizations) return false
        if (damageTakenOnTeamPercentage != other.damageTakenOnTeamPercentage) return false
        if (teamDamagePercentage != other.teamDamagePercentage) return false
        if (damagePerMinute != other.damagePerMinute) return false
        if (kda != other.kda) return false
        if (mmr != other.mmr) return false
        if (mmrFlat != other.mmrFlat) return false
        if (minionsKills != other.minionsKills) return false
        if (inhibitorKills != other.inhibitorKills) return false
        if (team != other.team) return false
        if (profileIcon != other.profileIcon) return false
        if (win != other.win) return false
        if (snowballsHit != other.snowballsHit) return false
        if (skillshotsHit != other.skillshotsHit) return false
        if (summonerLevel != other.summonerLevel) return false
        if (soloKills != other.soloKills) return false
        if (survivedSingleDigitHpCount != other.survivedSingleDigitHpCount) return false
        if (magicDamageDealtToChampions != other.magicDamageDealtToChampions) return false
        if (physicalDamageDealtToChampions != other.physicalDamageDealtToChampions) return false
        if (trueDamageDealtToChampions != other.trueDamageDealtToChampions) return false
        if (effectiveHealAndShielding != other.effectiveHealAndShielding) return false
        if (damageSelfMitigated != other.damageSelfMitigated) return false
        if (largestCriticalStrike != other.largestCriticalStrike) return false
        if (survivedThreeImmobilizesInFight != other.survivedThreeImmobilizesInFight) return false
        if (totalTimeCCDealt != other.totalTimeCCDealt) return false
        if (tookLargeDamageSurvived != other.tookLargeDamageSurvived) return false
        if (longestTimeSpentLiving != other.longestTimeSpentLiving) return false
        if (totalTimeSpentDead != other.totalTimeSpentDead) return false
        if (match_id != other.match_id) return false
        if (LOLperson_id != other.LOLperson_id) return false
        if (guild_id != other.guild_id) return false
        if (createdAt != other.createdAt) return false
        if (updatedAt != other.updatedAt) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + championId
        result = 31 * result + championName.hashCode()
        result = 31 * result + kills5
        result = 31 * result + kills4
        result = 31 * result + kills3
        result = 31 * result + kills2
        result = 31 * result + kills
        result = 31 * result + assists
        result = 31 * result + deaths
        result = 31 * result + goldEarned
        result = 31 * result + skillsCast
        result = 31 * result + totalDmgToChampions
        result = 31 * result + totalDamageShieldedOnTeammates
        result = 31 * result + totalHealsOnTeammates
        result = 31 * result + totalDamageTaken
        result = 31 * result + damageDealtToBuildings
        result = 31 * result + timeCCingOthers
        result = 31 * result + skillshotsDodged
        result = 31 * result + enemyChampionImmobilizations
        result = 31 * result + damageTakenOnTeamPercentage.hashCode()
        result = 31 * result + teamDamagePercentage.hashCode()
        result = 31 * result + damagePerMinute.hashCode()
        result = 31 * result + kda.hashCode()
        result = 31 * result + mmr.hashCode()
        result = 31 * result + mmrFlat.hashCode()
        result = 31 * result + minionsKills
        result = 31 * result + inhibitorKills
        result = 31 * result + team
        result = 31 * result + profileIcon
        result = 31 * result + win.hashCode()
        result = 31 * result + snowballsHit
        result = 31 * result + skillshotsHit
        result = 31 * result + summonerLevel
        result = 31 * result + soloKills
        result = 31 * result + survivedSingleDigitHpCount
        result = 31 * result + magicDamageDealtToChampions
        result = 31 * result + physicalDamageDealtToChampions
        result = 31 * result + trueDamageDealtToChampions
        result = 31 * result + effectiveHealAndShielding.hashCode()
        result = 31 * result + damageSelfMitigated
        result = 31 * result + largestCriticalStrike
        result = 31 * result + survivedThreeImmobilizesInFight
        result = 31 * result + totalTimeCCDealt
        result = 31 * result + tookLargeDamageSurvived
        result = 31 * result + longestTimeSpentLiving
        result = 31 * result + totalTimeSpentDead
        result = 31 * result + match_id
        result = 31 * result + LOLperson_id
        result = 31 * result + guild_id
        result = 31 * result + createdAt.hashCode()
        result = 31 * result + updatedAt.hashCode()
        return result
    }

    override fun toString(): String {
        return "Participants(id=$id, match_id=$match_id, LOLperson_id=$LOLperson_id, guild_id=$guild_id, championName='$championName')"
    }
}