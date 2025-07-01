package ru.descend.bot.postgre.r2dbc.model

import org.komapper.annotation.KomapperAutoIncrement
import org.komapper.annotation.KomapperEntity
import org.komapper.annotation.KomapperId
import org.komapper.annotation.KomapperTable
import org.komapper.core.dsl.Meta
import ru.descend.bot.datas.getDataOne
import ru.descend.bot.lolapi.dto.matchDto.Participant
import ru.descend.bot.postgre.R2DBC.stockHEROES
import ru.descend.bot.postgre.r2dbc.model.LOLs.Companion.tbl_lols
import ru.descend.bot.to1Digits

@KomapperEntity
@KomapperTable("tbl_participants_new")
data class ParticipantsNew(
    @KomapperId
    @KomapperAutoIncrement
    val id: Int = 0,

    var match_id: Int = -1,
    var LOLperson_id: Int = -1,

    var assists: Int = 0,
    var championId: Int = 0,
    var championName: String = "",
    var consumablesPurchased: Int = 0,
    var damageDealtToBuildings: Int = 0,
    var damageSelfMitigated: Int = 0,
    var deaths: Int = 0,
    var goldEarned: Int = 0,
    var goldSpent: Int = 0,
    var itemsPurchased: Int = 0,
    var kills: Int = 0,
    var largestCriticalStrike: Int = 0,
    var largestKillingSpree: Int = 0,
    var largestMultiKill: Int = 0,
    var longestTimeSpentLiving: Int = 0,
    var magicDamageDealt: Int = 0,
    var magicDamageDealtToChampions: Int = 0,
    var magicDamageTaken: Int = 0,
    var participantId: Int = 0,
    var physicalDamageDealt: Int = 0,
    var physicalDamageDealtToChampions: Int = 0,
    var physicalDamageTaken: Int = 0,
    var profileIcon: Int = 0,
    var puuid: String = "",
    var riotIdGameName: String = "",
    var riotIdTagline: String = "",
    var spell1Casts: Int = 0,
    var spell2Casts: Int = 0,
    var spell3Casts: Int = 0,
    var spell4Casts: Int = 0,
    var summoner1Casts: Int = 0,
    var summoner1Id: Int = 0,
    var summoner2Casts: Int = 0,
    var summoner2Id: Int = 0,
    var summonerLevel: Int = 0,
    var teamId: Int = 0,
    var timeCCingOthers: Int = 0,
    var timePlayed: Int = 0,
    var totalDamageDealt: Int = 0,
    var totalDamageDealtToChampions: Int = 0,
    var totalDamageShieldedOnTeammates: Int = 0,
    var totalDamageTaken: Int = 0,
    var totalHeal: Int = 0,
    var totalHealsOnTeammates: Int = 0,
    var totalMinionsKilled: Int = 0,
    var totalTimeCCDealt: Int = 0,
    var totalTimeSpentDead: Int = 0,
    var totalUnitsHealed: Int = 0,
    var trueDamageDealt: Int = 0,
    var trueDamageDealtToChampions: Int = 0,
    var trueDamageTaken: Int = 0,
    var turretKills: Int = 0,
    var turretsLost: Int = 0,
    var win: Boolean = false,
    var hadAfkTeammate: Int = 0,
    var abilityUses: Int = 0,
    var bountyGold: Double = 0.0,
    var damagePerMinute: Double = 0.0,
    var damageTakenOnTeamPercentage: Double = 0.0,
    var deathsByEnemyChamps: Int = 0,
    var dodgeSkillShotsSmallWindow: Int = 0,
    var effectiveHealAndShielding: Double = 0.0,
    var enemyChampionImmobilizations: Int = 0,
    var gameLength: Double = 0.0,
    var goldPerMinute: Double = 0.0,
    var immobilizeAndKillWithAlly: Int = 0,
    var kda: Double = 0.0,
    var killAfterHiddenWithAlly: Int = 0,
    var killedChampTookFullTeamDamageSurvived: Int = 0,
    var killingSprees: Int = 0,
    var killParticipation: Double = 0.0,
    var killsUnderOwnTurret: Int = 0,
    var laneMinionsFirst10Minutes: Int = 0,
    var multiKillOneSpell: Int = 0,
    var multikills: Int = 0,
    var outnumberedKills: Int = 0,
    var perfectGame: Int = 0,
    var pickKillWithAlly: Int = 0,
    var saveAllyFromDeath: Int = 0,
    var skillshotsDodged: Int = 0,
    var skillshotsHit: Int = 0,
    var snowballsHit: Int = 0,
    var soloKills: Int = 0,
    var survivedSingleDigitHpCount: Int = 0,
    var survivedThreeImmobilizesInFight: Int = 0,
    var takedownOnFirstTurret: Int = 0,
    var takedowns: Int = 0,
    var teamDamagePercentage: Double = 0.0,
    var tookLargeDamageSurvived: Int = 0,
    var turretTakedowns: Int = 0,

    var kills5: Int = 0,
    var kills4: Int = 0,
    var kills3: Int = 0,
    var kills2: Int = 0,

    var matchDateEnd: Long = 0,
    var gameMatchKey: String = "",
    var gameMatchMmr: Double = 0.0,
    var needCalcStats: Boolean = true,

) {
    var top_damagePerMinute = false
    var top_damageMitigated = false
    var top_kda = false
    var top_cc = false
    var top_creeps = false
    var top_healTeammates = false
    var top_goldPerMinute = false
    var top_accuracy = false

    var tempTextMMR2: String = ""
    var tempTextMMR2value: Double = 0.0
    constructor(participant: Participant, match: Matches, LOLperson: LOLs) : this() {
        val kill5 = participant.pentaKills
        val kill4 = participant.quadraKills - kill5
        val kill3 = participant.tripleKills - kill4
        val kill2 = participant.doubleKills - kill3

        this.match_id = match.id
        this.LOLperson_id = LOLperson.id
        this.matchDateEnd = match.matchDateEnd

        this.assists = participant.assists
        this.championId = participant.championId
        this.championName = participant.championName
        this.consumablesPurchased = participant.consumablesPurchased
        this.damageDealtToBuildings = participant.damageDealtToBuildings
        this.damageSelfMitigated = participant.damageSelfMitigated
        this.deaths = participant.deaths
        this.goldEarned = participant.goldEarned
        this.goldSpent = participant.goldSpent
        this.itemsPurchased = participant.itemsPurchased
        this.kills = participant.kills
        this.largestCriticalStrike = participant.largestCriticalStrike
        this.largestKillingSpree = participant.largestKillingSpree
        this.largestMultiKill = participant.largestMultiKill
        this.longestTimeSpentLiving = participant.longestTimeSpentLiving
        this.magicDamageDealt = participant.magicDamageDealt
        this.magicDamageDealtToChampions = participant.magicDamageDealtToChampions
        this.magicDamageTaken = participant.magicDamageTaken
        this.participantId = participant.participantId
        this.physicalDamageDealt = participant.physicalDamageDealt
        this.physicalDamageDealtToChampions = participant.physicalDamageDealtToChampions
        this.physicalDamageTaken = participant.physicalDamageTaken
        this.profileIcon = participant.profileIcon
        this.puuid = participant.puuid
        this.riotIdGameName = participant.riotIdGameName?:""
        this.riotIdTagline = participant.riotIdTagline?:""
        this.spell1Casts = participant.spell1Casts
        this.spell2Casts = participant.spell2Casts
        this.spell3Casts = participant.spell3Casts
        this.spell4Casts = participant.spell4Casts
        this.summoner1Casts = participant.summoner1Casts
        this.summoner1Id = participant.summoner1Id
        this.summoner2Casts = participant.summoner2Casts
        this.summoner2Id = participant.summoner2Id
        this.summonerLevel = participant.summonerLevel
        this.teamId = participant.teamId
        this.timeCCingOthers = participant.timeCCingOthers
        this.timePlayed = participant.timePlayed
        this.totalDamageDealt = participant.totalDamageDealt
        this.totalDamageDealtToChampions = participant.totalDamageDealtToChampions
        this.totalDamageShieldedOnTeammates = participant.totalDamageShieldedOnTeammates
        this.totalDamageTaken = participant.totalDamageTaken
        this.totalHeal = participant.totalHeal
        this.totalHealsOnTeammates = participant.totalHealsOnTeammates
        this.totalMinionsKilled = participant.totalMinionsKilled
        this.totalTimeCCDealt = participant.totalTimeCCDealt
        this.totalTimeSpentDead = participant.totalTimeSpentDead
        this.totalUnitsHealed = participant.totalUnitsHealed
        this.trueDamageDealt = participant.trueDamageDealt
        this.trueDamageDealtToChampions = participant.trueDamageDealtToChampions
        this.trueDamageTaken = participant.trueDamageTaken
        this.turretKills = participant.turretKills
        this.turretsLost = participant.turretsLost
        this.win = participant.win

        if (participant.challenges != null) {
            this.hadAfkTeammate = participant.challenges.hadAfkTeammate
            this.abilityUses = participant.challenges.abilityUses
            this.bountyGold = participant.challenges.bountyGold.to1Digits()
            this.damagePerMinute = participant.challenges.damagePerMinute.to1Digits()
            this.damageTakenOnTeamPercentage = participant.challenges.damageTakenOnTeamPercentage.to1Digits()
            this.deathsByEnemyChamps = participant.challenges.deathsByEnemyChamps
            this.dodgeSkillShotsSmallWindow = participant.challenges.dodgeSkillShotsSmallWindow
            this.effectiveHealAndShielding = participant.challenges.effectiveHealAndShielding.to1Digits()
            this.enemyChampionImmobilizations = participant.challenges.enemyChampionImmobilizations
            this.gameLength = participant.challenges.gameLength.to1Digits()
            this.goldPerMinute = participant.challenges.goldPerMinute.to1Digits()
            this.immobilizeAndKillWithAlly = participant.challenges.immobilizeAndKillWithAlly
            this.kda = participant.challenges.kda.to1Digits()
            this.killAfterHiddenWithAlly = participant.challenges.killAfterHiddenWithAlly
            this.killedChampTookFullTeamDamageSurvived = participant.challenges.killedChampTookFullTeamDamageSurvived
            this.killingSprees = participant.challenges.killingSprees
            this.killParticipation = participant.challenges.killParticipation.to1Digits()
            this.killsUnderOwnTurret = participant.challenges.killsUnderOwnTurret
            this.laneMinionsFirst10Minutes = participant.challenges.laneMinionsFirst10Minutes
            this.multiKillOneSpell = participant.challenges.multiKillOneSpell
            this.multikills = participant.challenges.multikills
            this.outnumberedKills = participant.challenges.outnumberedKills
            this.perfectGame = participant.challenges.perfectGame
            this.pickKillWithAlly = participant.challenges.pickKillWithAlly
            this.saveAllyFromDeath = participant.challenges.saveAllyFromDeath
            this.skillshotsDodged = participant.challenges.skillshotsDodged
            this.skillshotsHit = participant.challenges.skillshotsHit
            this.snowballsHit = participant.challenges.snowballsHit
            this.soloKills = participant.challenges.soloKills
            this.survivedSingleDigitHpCount = participant.challenges.survivedSingleDigitHpCount
            this.survivedThreeImmobilizesInFight = participant.challenges.survivedThreeImmobilizesInFight
            this.takedownOnFirstTurret = participant.challenges.takedownOnFirstTurret
            this.takedowns = participant.challenges.takedowns
            this.teamDamagePercentage = participant.challenges.teamDamagePercentage.to1Digits()
            this.tookLargeDamageSurvived = participant.challenges.tookLargeDamageSurvived
            this.turretTakedowns = participant.challenges.turretTakedowns
        }

        this.kills5 = kill5
        this.kills4 = kill4
        this.kills3 = kill3
        this.kills2 = kill2
        this.needCalcStats = match.isNeedCalcStats()
    }

    companion object {
        val tbl_participantsnew = Meta.participantsNew
    }

    suspend fun getHeroNameRU() = stockHEROES.get().find { it.key == championId.toString() }?.nameRU?:""

    suspend fun LOLpersonObj() = LOLs().getDataOne({ tbl_lols.id eq LOLperson_id })

    override fun toString(): String {
        val textLastMMR = if (gameMatchMmr != 0.0) ", gameMatchMmr='$gameMatchMmr'" else ""
        val textLastMMRKey = if (gameMatchKey != "") ", gameMatchKey='$gameMatchKey'" else ""
        return "ParticipantsNew(id=$id, LOL=$LOLperson_id, match_id=$match_id, championName='$championName'$textLastMMR$textLastMMRKey)"
    }
}