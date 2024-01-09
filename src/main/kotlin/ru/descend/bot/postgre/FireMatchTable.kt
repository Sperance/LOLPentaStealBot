package ru.descend.bot.postgre

import Entity
import column
import databases.Database
import ru.descend.bot.firebase.FireMatch
import save
import table

data class FireMatchTable(
    override var id: Int = 0,
    var matchId: String = "",
    var matchDate: Long = 0,
    var matchDuration: Long = 0,
    var matchMode: String = "",
    var matchGameVersion: String = "",

    var guild: FireGuildTable? = null
) : Entity() {

    val participants: List<FireParticipantTable> by oneToMany(FireParticipantTable::match)

    fun addParticipants(match: FireMatch) {
        match.listPerc.forEach {part ->
            FireParticipantTable(
                championId = part.championId,
                summonerId = part.summonerId,
                championName = part.championName,
                summonerName = part.summonerName,
                puuid = part.puuid,
                riotIdTagline = part.riotIdTagline,
                kills5 = part.kills5.toLong(),
                kills4 = part.kills4.toLong(),
                kills3 = part.kills3.toLong(),
                kills2 = part.kills2.toLong(),
                kills = part.kills.toLong(),
                assists = part.assists.toLong(),
                deaths = part.deaths.toLong(),
                goldEarned = part.goldEarned.toLong(),
                skillsCast = part.skillsCast.toLong(),
                totalDmgToChampions = part.totalDmgToChampions.toLong(),
                minionsKills = part.minionsKills.toLong(),
                team = part.team,
                win = part.win,
                match = this
            ).save()
        }
    }
}

val fireMatchTable = table<FireMatchTable, Database> {
    column(FireMatchTable::matchId).unique()
}