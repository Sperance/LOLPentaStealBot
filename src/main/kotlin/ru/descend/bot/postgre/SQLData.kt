package ru.descend.bot.postgre

import dev.kord.core.entity.Guild
import ru.descend.bot.lolapi.leaguedata.match_dto.MatchDTO
import ru.descend.bot.printLog

class SQLData (val guild: Guild, val guildSQL: TableGuild) {

    private val sqlCurrentMatches  = ArrayList<TableMatch>()
    fun addCurrentMatch(item: TableMatch?) {
        if (item != null) sqlCurrentMatches.add(item)
    }
    private val sqlCurrentKORD  = ArrayList<TableKORDPerson>()
    fun addCurrentKORD(item: TableKORDPerson?) {
        if (item != null) sqlCurrentKORD.add(item)
    }
    private val sqlCurrentLOL  = ArrayList<TableLOLPerson>()
    fun addCurrentLOL(item: TableLOLPerson?) {
        if (item != null) sqlCurrentLOL.add(item)
    }
    private val sqlCurrentKORDLOL  = ArrayList<TableKORD_LOL>()
    fun addCurrentKORDLOL(item: TableKORD_LOL?) {
        if (item != null) sqlCurrentKORDLOL.add(item)
    }
    private val sqlCurrentParticipants = ArrayList<TableParticipant>()
    fun addCurrentParticipant(item: TableParticipant?) {
        if (item != null) sqlCurrentParticipants.add(item)
    }

    fun reloadSQLData() {

        if (guildSQL.botChannelId.isEmpty()) return

        sqlCurrentMatches.clear()
        sqlCurrentMatches.addAll(tableMatch.getAll { TableMatch::guild eq guildSQL })

        sqlCurrentParticipants.clear()
        sqlCurrentParticipants.addAll(tableParticipant.getAll { TableParticipant::guildUid eq guildSQL.idGuild })
        sqlCurrentParticipants.sortByDescending { it.match?.matchDate }

        sqlCurrentKORDLOL.clear()
        sqlCurrentKORDLOL.addAll(tableKORDLOL.getAll { TableKORD_LOL::guild eq guildSQL })

        sqlCurrentKORD.clear()
        sqlCurrentKORD.addAll(tableKORDPerson.getAll { TableKORDPerson::guild eq guildSQL })

        sqlCurrentLOL.clear()
        sqlCurrentKORD.forEach {
            sqlCurrentLOL.addAll(it.LOLpersons)
        }
    }

    fun getKORDLOL() = sqlCurrentKORDLOL
    fun getLOL() = sqlCurrentLOL
    fun getMatches() = sqlCurrentMatches
    fun getParticipants() = sqlCurrentParticipants

    fun getSavedParticipants() : ArrayList<TableParticipant> {
        val result = ArrayList<TableParticipant>()
        result.addAll(getParticipants().filter { part -> getKORDLOL().find { it.LOLperson?.LOL_puuid == part.LOLperson?.LOL_puuid } != null })
        return result
    }

    fun isHaveMatchId(matchId: String) : Boolean {
        return getMatches().find { it.matchId == matchId } != null
    }
    fun addMatch(match: MatchDTO) {
        sqlCurrentMatches.add(guildSQL.addMatch(guild, match))
    }

    fun getParticipantsFromMatch(match: TableMatch): ArrayList<TableParticipant> {
        val result = ArrayList<TableParticipant>()
        result.addAll(sqlCurrentParticipants.filter { it.match?.matchId == match.matchId })
        return result
    }

    fun getSavedParticipantsFromMatch(match: TableMatch): ArrayList<TableParticipant> {
        val result = ArrayList<TableParticipant>()
        result.addAll(sqlCurrentParticipants.filter { it.match?.matchId == match.matchId && sqlCurrentKORDLOL.find { kl -> kl.LOLperson?.LOL_puuid == it.LOLperson?.LOL_puuid } != null })
        return result
    }

    fun getSavedParticipantsFromMatch(match: TableMatch, kordLol: TableKORD_LOL): TableParticipant? {
        return sqlCurrentParticipants.find { it.match?.matchId == match.matchId && it.LOLperson?.LOL_puuid == kordLol.LOLperson?.LOL_puuid }
    }

    fun getCountPenta(lolPerson: TableLOLPerson?) : Int {
        return sqlCurrentParticipants.filter { !it.match!!.bots && it.LOLperson?.LOL_puuid == lolPerson?.LOL_puuid }.sumOf { it.kills5 }
    }
    fun getCountPenta(lolPerson: TableKORD_LOL?) : Int {
        return sqlCurrentParticipants.filter { !it.match!!.bots && it.LOLperson?.LOL_puuid == lolPerson?.LOLperson?.LOL_puuid }.sumOf { it.kills5 }
    }

    fun getKORDLOLfromParticipant(participant: TableParticipant?) : TableKORD_LOL {
        if (participant == null) {
            throw IllegalArgumentException("[SQLData::getKORDLOLfromParticipant] participant is null")
        }
        if (participant.LOLperson == null) {
            throw IllegalArgumentException("[SQLData::getKORDLOLfromParticipant] LOLperson is null. Part: $participant")
        }

        val findedValue = sqlCurrentKORDLOL.find { it.LOLperson?.LOL_puuid == participant.LOLperson?.LOL_puuid }
        if (findedValue == null) {
            sqlCurrentKORDLOL.forEach {
                printLog("[SQLData::getKORDLOLfromParticipant] id KORDLOL: ${it.id} LOLPerson: ${it.LOLperson}")
            }
            throw IllegalArgumentException("[SQLData::getKORDLOLfromParticipant] Not find KORDLOL object from participant. Participant LOLperson: ${participant.LOLperson}")
        }

        return findedValue
    }
}