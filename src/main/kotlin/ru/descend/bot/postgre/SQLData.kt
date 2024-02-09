package ru.descend.bot.postgre

import dev.kord.core.entity.Guild
import ru.descend.bot.lolapi.leaguedata.match_dto.MatchDTO
import ru.descend.bot.postgre.tables.TableGuild
import ru.descend.bot.postgre.tables.TableKORDPerson
import ru.descend.bot.postgre.tables.TableKORD_LOL
import ru.descend.bot.postgre.tables.TableLOLPerson
import ru.descend.bot.postgre.tables.TableMatch
import ru.descend.bot.postgre.tables.TableMmr
import ru.descend.bot.postgre.tables.TableParticipant
import ru.descend.bot.postgre.tables.tableKORDLOL
import ru.descend.bot.postgre.tables.tableKORDPerson
import ru.descend.bot.postgre.tables.tableLOLPerson
import ru.descend.bot.postgre.tables.tableMatch
import ru.descend.bot.postgre.tables.tableMmr
import ru.descend.bot.postgre.tables.tableParticipant
import ru.descend.bot.printLog
import statements.select
import statements.selectAll

class SQLData (val guild: Guild, val guildSQL: TableGuild) {

    var isNeedUpdateMastery = true

    fun getKORDLOL() = tableKORDLOL.getAll { TableKORD_LOL::guild eq guildSQL }
    fun getMMR() = tableMmr.selectAll().getEntities()
    fun getLOL(): ArrayList<TableLOLPerson> {
        val list =  ArrayList<TableLOLPerson>()
        tableKORDLOL.selectAll().where { TableKORD_LOL::guild eq guildSQL }.getEntities().forEach {
            if (it.LOLperson != null) list.add(it.LOLperson!!)
        }
        return list
    }
    fun getAllLOL() = tableLOLPerson.getAll().filter { !it.isBot() }
    fun getKORD() = tableKORDPerson.getAll { TableKORDPerson::guild eq guildSQL }

    fun getLastParticipants(puuid: String?, limit: Int) : ArrayList<TableParticipant> {
        val result = ArrayList<TableParticipant>()
        try {
            result.addAll(
                tableParticipant.selectAll()
                    .where { TableParticipant::LOLperson eq (getLOL().find { it.LOL_puuid == puuid }?:"")}
                    .where { TableParticipant::bot eq false }
                    .where { TableMatch::bots eq false }
                    .where { TableMatch::surrender eq false }
                    .orderByDescending(TableParticipant::match)
                    .limit(limit)
                    .getEntities())
            result.sortBy { it.match?.matchId }
        }catch (e: Exception) {
            printLog(guild, "[getLastParticipants] Error: ${e.localizedMessage}")
        }
        return result
    }

    fun getSavedParticipants() : ArrayList<TableParticipant> {
        val result = ArrayList<TableParticipant>()
        try {
            result.addAll(tableParticipant.selectAll()
                .where { TableParticipant::LOLperson.inList(getKORDLOL().map { it.LOLperson?.id }) }
                .where { TableParticipant::bot eq false }
                .where { TableMatch::bots eq false }
                .where { TableMatch::surrender eq false }
                .getEntities())
            result.sortByDescending { it.match?.matchId }
        }catch (e: Error) {
            printLog(guild, "[getSavedParticipants] Error: ${e.localizedMessage}")
        }
        return result
    }

    suspend fun addMatch(match: MatchDTO, kordlol: List<TableKORD_LOL>? = null, tableMMR: List<TableMmr>? = null) {
        guildSQL.addMatch(guild, match, kordlol, tableMMR)
    }

    fun getNewMatches(list: ArrayList<String>): ArrayList<String> {
        tableMatch.select(TableMatch::matchId)
            .where { TableMatch::matchId.inList(list) }
            .where { TableMatch::guild eq guildSQL }
            .getEntities()
            .forEach {
                list.remove(it.matchId)
        }
        return list
    }

    fun getKORDLOLfromParticipant(kordlol: List<TableKORD_LOL>, participant: TableParticipant?) : TableKORD_LOL {
        if (participant == null) {
            throw IllegalArgumentException("[SQLData::getKORDLOLfromParticipant] participant is null")
        }
        if (participant.LOLperson == null) {
            throw IllegalArgumentException("[SQLData::getKORDLOLfromParticipant] LOLperson is null. Part: $participant")
        }

        val findedValue = kordlol.find { it.LOLperson?.LOL_puuid == participant.LOLperson?.LOL_puuid }
        if (findedValue == null) {
            kordlol.forEach {
                printLog("[SQLData::getKORDLOLfromParticipant] id KORDLOL: ${it.id} LOLPerson: ${it.LOLperson}")
            }
            throw IllegalArgumentException("[SQLData::getKORDLOLfromParticipant] Not find KORDLOL object from participant. Participant LOLperson: ${participant.LOLperson}")
        }

        return findedValue
    }
}