package ru.descend.bot.postgre

import Entity
import column
import databases.Database
import ru.descend.bot.lolapi.leaguedata.match_dto.Participant
import table

data class FireParticipantTable(
    override var id: Int = 0,
    var championId: Int = -1,
    var championName: String = "",
    var kills5: Long = 0,
    var kills4: Long = 0,
    var kills3: Long = 0,
    var kills2: Long = 0,
    var kills: Long = 0,
    var assists: Long = 0,
    var deaths: Long = 0,
    var goldEarned: Long = 0,
    var skillsCast: Long = 0,
    var totalDmgToChampions: Long = 0,
    var minionsKills: Long = 0,
    var team: Int = -1,
    var win: Boolean = false,

    var match: FireMatchTable? = null,
    var LOLperson: FireLOLPersonTable? = null
) : Entity()

val fireParticipantTable = table<FireParticipantTable, Database>{
    column(FireParticipantTable::match).check { it neq 0 }
}