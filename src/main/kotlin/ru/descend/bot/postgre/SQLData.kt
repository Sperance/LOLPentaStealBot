package ru.descend.bot.postgre

import dev.kord.core.entity.Guild
import ru.descend.bot.lolapi.leaguedata.match_dto.MatchDTO
import ru.descend.bot.postgre.tables.TableGuild
import ru.descend.bot.postgre.tables.TableKORDPerson
import ru.descend.bot.postgre.tables.TableKORD_LOL
import ru.descend.bot.postgre.tables.TableLOLPerson
import ru.descend.bot.postgre.tables.TableMatch
import ru.descend.bot.postgre.tables.TableParticipant
import ru.descend.bot.postgre.tables.tableKORDLOL
import ru.descend.bot.postgre.tables.tableKORDPerson
import ru.descend.bot.postgre.tables.tableLOLPerson
import ru.descend.bot.postgre.tables.tableMatch
import ru.descend.bot.postgre.tables.tableParticipant
import ru.descend.bot.printLog
import statements.select
import statements.selectAll

class SQLData (val guild: Guild, val guildSQL: TableGuild) {

    private val sqlCurrentKORD  = ArrayList<TableKORDPerson>()
    fun addCurrentKORD(item: TableKORDPerson?) {
        if (item != null) sqlCurrentKORD.add(item)
    }
    private val sqlAllLOL  = ArrayList<TableLOLPerson>()
    fun addAllLOL(item: TableLOLPerson?) {
        if (item != null) sqlAllLOL.add(item)
    }
    private val sqlCurrentLOL  = ArrayList<TableLOLPerson>()
    fun addCurrentLOL(item: TableLOLPerson?) {
        if (item != null) sqlCurrentLOL.add(item)
    }
    private val sqlCurrentKORDLOL  = ArrayList<TableKORD_LOL>()
    fun addCurrentKORDLOL(item: TableKORD_LOL?) {
        if (item != null) sqlCurrentKORDLOL.add(item)
    }

    fun reloadSQLData() {

        if (guildSQL.botChannelId.isEmpty()) return

        sqlCurrentKORDLOL.clear()
        sqlCurrentKORDLOL.addAll(tableKORDLOL.getAll { TableKORD_LOL::guild eq guildSQL })

        sqlCurrentKORD.clear()
        sqlCurrentKORD.addAll(tableKORDPerson.getAll { TableKORDPerson::guild eq guildSQL })

        sqlAllLOL.clear()
        sqlAllLOL.addAll(tableLOLPerson.getAll().filter { !it.isBot() })

        sqlCurrentLOL.clear()
        sqlCurrentKORD.forEach {
            sqlCurrentLOL.addAll(it.LOLpersons)
        }
    }

    fun getKORDLOL() = sqlCurrentKORDLOL
    fun getLOL() = sqlCurrentLOL
    fun getAllLOL() = sqlAllLOL
    fun getKORD() = sqlCurrentKORD

    fun getLastParticipants(puuid: String?, limit: Int) : ArrayList<TableParticipant> {
        val result = ArrayList<TableParticipant>()
        result.addAll(
            tableParticipant.selectAll().where { TableParticipant::LOLperson eq getLOL().find { it.LOL_puuid == puuid } }.where { TableMatch::bots eq false }.orderByDescending(
                TableParticipant::match).limit(limit).getEntities())
        result.sortBy { it.match?.matchId }
        return result
    }

    fun getSavedParticipants() : ArrayList<TableParticipant> {
        val result = ArrayList<TableParticipant>()
        result.addAll(tableParticipant.selectAll().where { TableParticipant::LOLperson.inList(getKORDLOL().map { it.LOLperson?.id }) }.where { TableMatch::bots eq false }.getEntities())
        result.sortByDescending { it.match?.matchId }
        return result
    }

    fun addMatch(match: MatchDTO) {
        guildSQL.addMatch(guild, match)
    }

    fun getNewMatches(list: ArrayList<String>): ArrayList<String> {
        tableMatch.select(TableMatch::matchId).where { TableMatch::matchId.inList(list) }.where { TableMatch::guild eq guildSQL }.getEntities().forEach {
            list.remove(it.matchId)
        }
        return list
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