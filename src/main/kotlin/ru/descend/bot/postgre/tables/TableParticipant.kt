package ru.descend.bot.postgre.tables

import Entity
import column
import databases.Database
import ru.descend.bot.lolapi.leaguedata.match_dto.Participant
import statements.selectAll
import table

data class TableParticipantData(
    var part: TableParticipant? = null,
    var statWins: Int = 0,
    var statGames: Int = 0
){
    fun clearData() {
        statWins = 0
        statGames = 0
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TableParticipantData

        if (part != other.part) return false
        if (statWins != other.statWins) return false
        if (statGames != other.statGames) return false

        return true
    }

    override fun hashCode(): Int {
        var result = part?.hashCode() ?: 0
        result = 31 * result + statWins
        result = 31 * result + statGames
        return result
    }
}

data class TableParticipant(
    override var id: Int = 0,
    var participant_uid: String = "",
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
    var killParticipation: Double = 0.0,
    var kda: Double = 0.0,
    var mmr: Double = 0.0,
    var minionsKills: Int = 0,
    var baronKills: Int = 0,
    var dragonKills: Int = 0,
    var inhibitorKills: Int = 0,
    var nexusKills: Int = 0,
    var item0: Int = -1,
    var item1: Int = -1,
    var item2: Int = -1,
    var item3: Int = -1,
    var item4: Int = -1,
    var item5: Int = -1,
    var item6: Int = -1,
    var team: Int = -1,
    var profileIcon: Int = -1,
    var guildUid: String = "",
    var win: Boolean = false,
    var bot: Boolean = false,

    var match: TableMatch? = null,
    var LOLperson: TableLOLPerson? = null
) : Entity() {
    constructor(participant: Participant, match: TableMatch, LOLperson: TableLOLPerson) : this() {
        val kill5 = participant.pentaKills
        val kill4 = participant.quadraKills - kill5
        val kill3 = participant.tripleKills - kill4
        val kill2 = participant.doubleKills - kill3

        this.match = match
        this.LOLperson = LOLperson
        this.guildUid = match.guild!!.idGuild

        this.participant_uid = match.matchId + "#" + participant.puuid + "#" + LOLperson.id
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
        this.skillsCast = if (participant.challenges == null) 0 else participant.challenges.abilityUses.toInt()
        this.totalDmgToChampions = participant.totalDamageDealtToChampions
        this.totalHealsOnTeammates = participant.totalHealsOnTeammates
        this.totalDamageShieldedOnTeammates = participant.totalDamageShieldedOnTeammates
        this.totalDamageTaken = participant.totalDamageTaken
        this.damageDealtToBuildings = participant.damageDealtToBuildings
        this.timeCCingOthers = participant.timeCCingOthers
        this.skillshotsDodged = if (participant.challenges == null) 0 else participant.challenges.skillshotsDodged.toInt()
        this.enemyChampionImmobilizations = if (participant.challenges == null) 0 else participant.challenges.enemyChampionImmobilizations.toInt()
        this.damageTakenOnTeamPercentage = if (participant.challenges == null) 0.0 else participant.challenges.damageTakenOnTeamPercentage
        this.damagePerMinute = if (participant.challenges == null) 0.0 else participant.challenges.damagePerMinute
        this.killParticipation = if (participant.challenges == null) 0.0 else participant.challenges.killParticipation
        this.kda = if (participant.challenges == null) 0.0 else participant.challenges.kda
        this.teamDamagePercentage = if (participant.challenges == null) 0.0 else participant.challenges.teamDamagePercentage
        this.minionsKills = participant.totalMinionsKilled
        this.baronKills = participant.baronKills
        this.dragonKills = participant.dragonKills
        this.inhibitorKills = participant.inhibitorKills
        this.nexusKills = participant.nexusKills
        this.item0 = participant.item0
        this.item1 = participant.item1
        this.item2 = participant.item2
        this.item3 = participant.item3
        this.item4 = participant.item4
        this.item5 = participant.item5
        this.item6 = participant.item6
        this.profileIcon = participant.profileIcon
        this.team = participant.teamId
        this.win = participant.win
        this.bot = isBot()
    }

    private fun isBot() : Boolean {
        if (LOLperson == null) {
            throw IllegalAccessException("LOLperson is NULL. Participant id: $id")
        }
        if (LOLperson?.LOL_puuid == "BOT" || LOLperson?.LOL_puuid?.length!! < 5){
            return true
        }
        if (LOLperson?.LOL_summonerId == "BOT" || LOLperson?.LOL_summonerId?.length!! < 5){
            return true
        }
        return false
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TableParticipant

        if (id != other.id) return false
        if (participant_uid != other.participant_uid) return false
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
        if (killParticipation != other.killParticipation) return false
        if (kda != other.kda) return false
        if (mmr != other.mmr) return false
        if (minionsKills != other.minionsKills) return false
        if (baronKills != other.baronKills) return false
        if (dragonKills != other.dragonKills) return false
        if (inhibitorKills != other.inhibitorKills) return false
        if (nexusKills != other.nexusKills) return false
        if (item0 != other.item0) return false
        if (item1 != other.item1) return false
        if (item2 != other.item2) return false
        if (item3 != other.item3) return false
        if (item4 != other.item4) return false
        if (item5 != other.item5) return false
        if (item6 != other.item6) return false
        if (team != other.team) return false
        if (profileIcon != other.profileIcon) return false
        if (guildUid != other.guildUid) return false
        if (win != other.win) return false
        if (bot != other.bot) return false
        if (match != other.match) return false
        if (LOLperson != other.LOLperson) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + participant_uid.hashCode()
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
        result = 31 * result + killParticipation.hashCode()
        result = 31 * result + kda.hashCode()
        result = 31 * result + mmr.hashCode()
        result = 31 * result + minionsKills
        result = 31 * result + baronKills
        result = 31 * result + dragonKills
        result = 31 * result + inhibitorKills
        result = 31 * result + nexusKills
        result = 31 * result + item0
        result = 31 * result + item1
        result = 31 * result + item2
        result = 31 * result + item3
        result = 31 * result + item4
        result = 31 * result + item5
        result = 31 * result + item6
        result = 31 * result + team
        result = 31 * result + profileIcon
        result = 31 * result + guildUid.hashCode()
        result = 31 * result + win.hashCode()
        result = 31 * result + bot.hashCode()
        result = 31 * result + (match?.hashCode() ?: 0)
        result = 31 * result + (LOLperson?.hashCode() ?: 0)
        return result
    }
}

val tableParticipant = table<TableParticipant, Database>{
    column(TableParticipant::match).check { it neq 0 }
    column(TableParticipant::LOLperson).check { it neq null }
    column(TableParticipant::participant_uid).unique()
}