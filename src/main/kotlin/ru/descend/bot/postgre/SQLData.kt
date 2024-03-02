package ru.descend.bot.postgre

import dev.kord.core.entity.Guild
import kotlinx.coroutines.Job
import ru.descend.bot.lolapi.leaguedata.match_dto.MatchDTO
import ru.descend.bot.postgre.tables.TableGuild
import ru.descend.bot.postgre.tables.TableKORDPerson
import ru.descend.bot.postgre.tables.TableKORD_LOL
import ru.descend.bot.postgre.tables.TableLOLPerson
import ru.descend.bot.postgre.tables.TableMatch
import ru.descend.bot.postgre.tables.TableParticipant
import ru.descend.bot.postgre.tables.TableParticipantData
import ru.descend.bot.postgre.tables.tableKORDLOL
import ru.descend.bot.postgre.tables.tableKORDPerson
import ru.descend.bot.postgre.tables.tableMatch
import ru.descend.bot.postgre.tables.tableMmr
import ru.descend.bot.postgre.tables.tableParticipant
import ru.descend.bot.printLog
import statements.select
import statements.selectAll
import java.lang.ref.WeakReference
import java.util.WeakHashMap
import kotlin.concurrent.getOrSet

data class kordTemp(var kordLOL: TableKORD_LOL, var lastParts: ArrayList<TableParticipant>, var isBold: Boolean)
data class statMainTemp(var kord_lol_id: Int, var games: Int, var win: Int, var kill: Int, var kill2: Int, var kill3: Int, var kill4: Int, var kill5: Int)

class SQLData (var guild: Guild, var guildSQL: TableGuild) {

    private var _listKordTemp = ArrayList<kordTemp>()
    var listKordTemp = WeakReference(_listKordTemp)

    private var _listMainData = ArrayList<TableKORD_LOL>()
    var listMainData = WeakReference(_listMainData)

    private var _listCurrentUsers = ArrayList<String?>()
    var listCurrentUsers = WeakReference(_listCurrentUsers)

    var mainDataList1: ThreadLocal<java.util.ArrayList<Any?>> = ThreadLocal.withInitial{ ArrayList() }
    var mainDataList2: ThreadLocal<java.util.ArrayList<Any?>> = ThreadLocal.withInitial{ ArrayList() }
    var mainDataList3: ThreadLocal<java.util.ArrayList<Any?>> = ThreadLocal.withInitial{ ArrayList() }

    fun performClear() {
        clearDataList()
        clearPGData()
        clearMainDataList()

        listKordTemp.clear()
        listMainData.clear()
        listCurrentUsers.clear()

        arrayKORDLOL.clear()
        arraySavedParticipants.clear()
        mapWinStreak.clear()

        mainDataList1.remove()
        mainDataList2.remove()
        mainDataList3.remove()
    }

    fun clearMainDataList() {
        mainDataList1.get()?.clear()
        mainDataList2.get()?.clear()
        mainDataList3.get()?.clear()
    }

    fun initDataList() {
        clearDataList()
        clearPGData()
    }

    private fun clearDataList() {
        _listKordTemp.clear()
        _listMainData.clear()
        _listCurrentUsers.clear()
    }

    private fun clearPGData() {
        _arrayKORDLOL.clear()
        _arraySavedParticipants.clear()
        _mapWinStreak.clear()
    }

    private var _arrayKORDLOL = ArrayList<TableKORD_LOL>()
    private var arrayKORDLOL = WeakReference(_arrayKORDLOL)

    fun getKORDLOL(): ArrayList<TableKORD_LOL> {
        if (arrayKORDLOL.get().isNullOrEmpty()) {
            resetKORDLOL()
        }
        return arrayKORDLOL.get()!!
    }
    fun resetKORDLOL() {
        _arrayKORDLOL.clear()
        arrayKORDLOL.get()?.addAll(tableKORDLOL.getAll { TableKORD_LOL::guild eq guildSQL })
    }

    private fun getMMR() = tableMmr.selectAll().getEntities()
    fun getLOL(): ArrayList<TableLOLPerson> {
        val list =  ArrayList<TableLOLPerson>()
        tableKORDLOL.selectAll().where { TableKORD_LOL::guild eq guildSQL }.where { TableKORD_LOL::LOLperson neq null }.getEntities().forEach {
            list.add(it.LOLperson!!)
        }
        return list
    }
    fun getKORD() = tableKORDPerson.getAll { TableKORDPerson::guild eq guildSQL }

    private var _arraySavedParticipants = ArrayList<statMainTemp>()
    private var arraySavedParticipants = WeakReference(_arraySavedParticipants)

    fun resetSavedParticipants() {
        _arraySavedParticipants.clear()
        execQuery("SELECT * FROM get_player_stats()") {
            it?.let {
                while (it.next()){
                    val id = it.getInt("id")
                    val games = it.getInt("games")
                    val win = it.getInt("win")
                    val kill = it.getInt("kill")
                    val kill2 = it.getInt("kill2")
                    val kill3 = it.getInt("kill3")
                    val kill4 = it.getInt("kill4")
                    val kill5 = it.getInt("kill5")
                    arraySavedParticipants.get()?.add(statMainTemp(id, games, win, kill, kill2, kill3, kill4, kill5))
                }
            }
        }
    }
    fun getSavedParticipants() : ArrayList<statMainTemp> {
        if (arraySavedParticipants.get().isNullOrEmpty()) {
            resetSavedParticipants()
        }
        return arraySavedParticipants.get()!!
    }

    suspend fun addMatch(match: MatchDTO) {
        guildSQL.addMatch(this, match, getKORDLOL(), getMMR())
    }

    private var _mapWinStreak = WeakHashMap<Int, Int>()
    private var mapWinStreak = WeakReference(_mapWinStreak)

    fun getWinStreak() : WeakHashMap<Int, Int> {
        if (mapWinStreak.get().isNullOrEmpty()) {
            resetWinStreak()
        }
        return mapWinStreak.get()!!
    }
    fun resetWinStreak() {
        _mapWinStreak.clear()
        execQuery("SELECT * FROM get_streak_results()"){
            it?.let {
                while (it.next()){
                    val pers = it.getInt("PERS")
                    val res = it.getInt("RES")
                    val ZN = it.getString("ZN")
                    if (ZN == "+") {
                        mapWinStreak.get()?.put(pers, res)
                    } else if (ZN == "-") {
                        mapWinStreak.get()?.put(pers, -res)
                    }
                }
            }
        }
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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SQLData

        if (guild != other.guild) return false
        if (guildSQL != other.guildSQL) return false
        if (_listKordTemp != other._listKordTemp) return false
        if (listKordTemp != other.listKordTemp) return false
        if (_listMainData != other._listMainData) return false
        if (listMainData != other.listMainData) return false
        if (_listCurrentUsers != other._listCurrentUsers) return false
        if (listCurrentUsers != other.listCurrentUsers) return false
        if (mainDataList1 != other.mainDataList1) return false
        if (mainDataList2 != other.mainDataList2) return false
        if (mainDataList3 != other.mainDataList3) return false
        if (_arrayKORDLOL != other._arrayKORDLOL) return false
        if (arrayKORDLOL != other.arrayKORDLOL) return false
        if (_arraySavedParticipants != other._arraySavedParticipants) return false
        if (arraySavedParticipants != other.arraySavedParticipants) return false
        if (_mapWinStreak != other._mapWinStreak) return false
        if (mapWinStreak != other.mapWinStreak) return false

        return true
    }

    override fun hashCode(): Int {
        var result = guild.hashCode()
        result = 31 * result + guildSQL.hashCode()
        result = 31 * result + _listKordTemp.hashCode()
        result = 31 * result + listKordTemp.hashCode()
        result = 31 * result + _listMainData.hashCode()
        result = 31 * result + listMainData.hashCode()
        result = 31 * result + _listCurrentUsers.hashCode()
        result = 31 * result + listCurrentUsers.hashCode()
        result = 31 * result + mainDataList1.hashCode()
        result = 31 * result + mainDataList2.hashCode()
        result = 31 * result + mainDataList3.hashCode()
        result = 31 * result + _arrayKORDLOL.hashCode()
        result = 31 * result + arrayKORDLOL.hashCode()
        result = 31 * result + _arraySavedParticipants.hashCode()
        result = 31 * result + arraySavedParticipants.hashCode()
        result = 31 * result + _mapWinStreak.hashCode()
        result = 31 * result + mapWinStreak.hashCode()
        return result
    }
}