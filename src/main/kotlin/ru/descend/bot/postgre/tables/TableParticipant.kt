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
}

val tableParticipant = table<TableParticipant, Database>{
    column(TableParticipant::match).check { it neq 0 }
    column(TableParticipant::LOLperson).check { it neq null }
    column(TableParticipant::participant_uid).unique()
}