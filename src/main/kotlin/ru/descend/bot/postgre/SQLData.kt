package ru.descend.bot.postgre

import dev.kord.core.entity.Guild
import kotlinx.coroutines.delay
import ru.descend.bot.lolapi.LeagueMainObject
import ru.descend.bot.lolapi.leaguedata.match_dto.MatchDTO
import ru.descend.bot.postgre.tables.TableGuild
import ru.descend.bot.postgre.tables.TableKORDPerson
import ru.descend.bot.postgre.tables.TableKORD_LOL
import ru.descend.bot.postgre.tables.TableLOLPerson
import ru.descend.bot.postgre.tables.TableMatch
import ru.descend.bot.postgre.tables.TableParticipant
import ru.descend.bot.postgre.tables.tableKORDLOL
import ru.descend.bot.postgre.tables.tableKORDPerson
import ru.descend.bot.postgre.tables.tableMatch
import ru.descend.bot.postgre.tables.tableParticipant
import statements.select
import statements.selectAll
import java.lang.ref.WeakReference
import java.util.WeakHashMap

data class kordTemp(var kordLOL: TableKORD_LOL, var lastParts: ArrayList<TableParticipant>, var isBold: Boolean)
data class statMainTemp(var kord_lol_id: Int, var games: Int, var win: Int, var kill: Int, var kill2: Int, var kill3: Int, var kill4: Int, var kill5: Int)
data class statAramDataTemp(var kord_lol_id: Int, var mmr_aram: Double, var mmr_aram_saved: Double, var games: Int?, var champion_id: Int?, var mmr: Double?, var match_id: String?, var last_match_id: String?, var bold: Boolean)

class SQLData (var guild: Guild, var guildSQL: TableGuild) {

    private var _listKordTemp = ArrayList<kordTemp>()
    var listKordTemp = WeakReference(_listKordTemp)

    private var _listMainData = ArrayList<TableKORD_LOL>()
    var listMainData = WeakReference(_listMainData)

    var mainDataList1: ThreadLocal<java.util.ArrayList<Any?>> = ThreadLocal.withInitial{ ArrayList() }
    var mainDataList2: ThreadLocal<java.util.ArrayList<Any?>> = ThreadLocal.withInitial{ ArrayList() }
    var mainDataList3: ThreadLocal<java.util.ArrayList<Any?>> = ThreadLocal.withInitial{ ArrayList() }

    fun performClear() {
        clearDataList()
        clearPGData()
        clearMainDataList()

        listKordTemp.clear()
        listMainData.clear()

        arrayKORDLOL.clear()
        arraySavedParticipants.clear()
        arrayAramMMRData.clear()
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
    }

    private fun clearPGData() {
        _arrayKORDLOL.clear()
        _arraySavedParticipants.clear()
        _arrayAramMMRData.clear()
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

//    private fun getMMR() = tableMmr.selectAll().getEntities()
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

    suspend fun resetSavedParticipants() {
        _arraySavedParticipants.clear()
        execQuery("SELECT * FROM get_player_stats_param(${guildSQL.id})") {
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
    suspend fun getSavedParticipants() : ArrayList<statMainTemp> {
        if (arraySavedParticipants.get().isNullOrEmpty()) {
            resetSavedParticipants()
        }
        return arraySavedParticipants.get()!!
    }

    private var _arrayAramMMRData = ArrayList<statAramDataTemp>()
    private var arrayAramMMRData = WeakReference(_arrayAramMMRData)

    suspend fun resetArrayAramMMRData() {
        _arrayAramMMRData.clear()
        execQuery("SELECT * FROM get_aram_data_param(${guildSQL.id})") {
            it?.let {
                while (it.next()){
                    val id = it.getInt("id")
                    val mmr_aram = it.getDouble("mmr_aram")
                    val mmr_aram_saved = it.getDouble("mmr_aram_saved")
                    val games = it.getInt("games")
                    val champion_id = it.getInt("champion_id")
                    val mmr = it.getDouble("mmr")
                    val match_id = it.getString("match_id")
                    val last_match_id = it.getString("last_match_id")
                    arrayAramMMRData.get()?.add(statAramDataTemp(id, mmr_aram, mmr_aram_saved, games, champion_id, mmr, match_id, last_match_id, false))
                }
            }
        }
    }
    suspend fun getArrayAramMMRData() : ArrayList<statAramDataTemp> {
        if (arrayAramMMRData.get().isNullOrEmpty()) {
            resetArrayAramMMRData()
        }
        return arrayAramMMRData.get()!!
    }

    suspend fun addMatch(match: MatchDTO) {
        guildSQL.addMatch(this, match, getKORDLOL())
        calculatePentaSteal(match)
    }

    private suspend fun calculatePentaSteal(match: MatchDTO) {
        var isNeedCalculate = false
        match.info.participants.forEach {
            if (it.quadraKills - it.pentaKills > 0) {
                isNeedCalculate = true
                return@forEach
            }
        }
        if (isNeedCalculate) {
            val parts = tableParticipant.selectAll().where { TableMatch::matchId eq match.metadata.matchId }.getEntities()
            LeagueMainObject.catchPentaSteal(match.metadata.matchId).forEach {pair ->
                val firstPart = parts.find { it.puuid == pair.first }
                var secondPart = parts.find { it.puuid == pair.second }

                if (firstPart == null) {
                    guildSQL.sendEmail("Error", "Participant with PUUID ${pair.first} not found in SQL. Match:${match.metadata.matchId}")
                    return
                }

                if (secondPart == null) {
                    guildSQL.sendEmail("Error", "Participant with PUUID ${pair.second} not found in SQL. Match:${match.metadata.matchId}")
                    return
                }

                if (firstPart == secondPart) {
                    guildSQL.sendEmail("Error", "Participants with PUUID ${pair.first} are Equals. Match:${match.metadata.matchId}")
                    return
                }

                if (getKORDLOL().find { it.LOLperson?.LOL_puuid == firstPart.puuid } == null && getKORDLOL().find { it.LOLperson?.LOL_puuid == secondPart.puuid } == null) {
                    guildSQL.sendEmail("PENTASTEAL (${match.metadata.matchId})", "ХЗ какой чел ${firstPart.LOLperson?.LOL_riotIdName} на ${firstPart.championName} состили Пенту у хз кого ${secondPart.LOLperson?.LOL_riotIdName} на ${secondPart.championName}")
                    return
                }

                guildSQL.sendEmail("PENTASTEAL (${match.metadata.matchId})", "Чел ${firstPart.LOLperson?.LOL_riotIdName} на ${firstPart.championName} состили Пенту у ${secondPart.LOLperson?.LOL_riotIdName} на ${secondPart.championName}")
            }
        }
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
        execQuery("SELECT * FROM get_streak_results_param(${guildSQL.id})"){
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
        if (mainDataList1 != other.mainDataList1) return false
        if (mainDataList2 != other.mainDataList2) return false
        if (mainDataList3 != other.mainDataList3) return false
        if (_arrayKORDLOL != other._arrayKORDLOL) return false
        if (arrayKORDLOL != other.arrayKORDLOL) return false
        if (_arraySavedParticipants != other._arraySavedParticipants) return false
        if (arraySavedParticipants != other.arraySavedParticipants) return false
        if (_arrayAramMMRData != other._arrayAramMMRData) return false
        if (arrayAramMMRData != other.arrayAramMMRData) return false
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
        result = 31 * result + mainDataList1.hashCode()
        result = 31 * result + mainDataList2.hashCode()
        result = 31 * result + mainDataList3.hashCode()
        result = 31 * result + _arrayKORDLOL.hashCode()
        result = 31 * result + arrayKORDLOL.hashCode()
        result = 31 * result + _arraySavedParticipants.hashCode()
        result = 31 * result + arraySavedParticipants.hashCode()
        result = 31 * result + _arrayAramMMRData.hashCode()
        result = 31 * result + arrayAramMMRData.hashCode()
        result = 31 * result + _mapWinStreak.hashCode()
        result = 31 * result + mapWinStreak.hashCode()
        return result
    }
}