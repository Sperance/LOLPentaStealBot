package ru.descend.bot.postgre

import dev.kord.core.entity.Guild
import org.komapper.core.dsl.QueryDsl
import org.komapper.core.dsl.query.double
import org.komapper.core.dsl.query.int
import org.komapper.core.dsl.query.string
import ru.descend.bot.asyncLaunch
import ru.descend.bot.lolapi.LeagueMainObject
import ru.descend.bot.lolapi.dto.match_dto.MatchDTO
import ru.descend.bot.postgre.calculating.Calc_AddMatch
import ru.descend.bot.postgre.calculating.Calc_Birthday
import ru.descend.bot.datas.WorkData
import ru.descend.bot.launch
import ru.descend.bot.postgre.r2dbc.model.Guilds
import ru.descend.bot.postgre.r2dbc.model.KORDLOLs
import ru.descend.bot.postgre.r2dbc.model.KORDLOLs.Companion.tbl_kordlols
import ru.descend.bot.postgre.r2dbc.model.KORDs
import ru.descend.bot.postgre.r2dbc.model.KORDs.Companion.tbl_kords
import ru.descend.bot.postgre.r2dbc.model.LOLs
import ru.descend.bot.postgre.r2dbc.model.LOLs.Companion.tbl_lols
import ru.descend.bot.postgre.r2dbc.model.MMRs
import ru.descend.bot.postgre.r2dbc.model.Matches.Companion.tbl_matches
import ru.descend.bot.postgre.r2dbc.model.Participants
import ru.descend.bot.postgre.r2dbc.model.Participants.Companion.tbl_participants

data class statMainTemp_r2(var kord_lol_id: Int, var games: Int, var win: Int, var kill: Int, var kill2: Int, var kill3: Int, var kill4: Int, var kill5: Int, var kordLOL: KORDLOLs?)
data class statAramDataTemp_r2(var kord_lol_id: Int, var mmr_aram: Double, var mmr_aram_saved: Double, var games: Int, var champion_id: Int?, var mmr: Double?, var match_id: String?, var last_match_id: String?, var mvp_lvp_info: String?, var bold: Boolean, var kordLOL: KORDLOLs?)

class SQLData_R2DBC (var guild: Guild, var guildSQL: Guilds) {

    var isNeedUpdateDatas = false
    var isNeedUpdateDays = false

    val dataKORDLOL = WorkData<KORDLOLs>("KORDLOL")
    val dataKORD = WorkData<KORDs>("KORD")
    val dataMMR = WorkData<MMRs>("MMR")

    val dataSavedLOL = WorkData<LOLs>("SavedLOL")
    val dataSavedParticipants = WorkData<Participants>("SavedParticipants")

    fun initialize() {
        if (dataKORDLOL.bodyReset == null) dataKORDLOL.bodyReset = { R2DBC.getKORDLOLs { tbl_kordlols.guild_id eq guildSQL.id } }
        if (dataKORD.bodyReset == null) dataKORD.bodyReset = { R2DBC.getKORDs { tbl_kords.guild_id eq guildSQL.id } }
        if (dataMMR.bodyReset == null) dataMMR.bodyReset = { R2DBC.getMMRs(null) }

        if (dataSavedLOL.bodyReset == null) {
            dataSavedLOL.bodyReset = {
                val kordLol_lol_id = dataKORDLOL.get().map { it.LOL_id }
                R2DBC.runQuery(QueryDsl.from(tbl_lols).where { tbl_lols.id.inList(kordLol_lol_id) })
            }
        }

        if (dataSavedParticipants.bodyReset == null) {
            dataSavedParticipants.bodyReset = {
                val kordLol_lol_id = dataKORDLOL.get().map { it.LOL_id }
                R2DBC.runQuery(QueryDsl.from(tbl_participants).where { tbl_participants.LOLperson_id.inList(kordLol_lol_id) })
            }
        }
    }

    suspend fun getKORDLOL(reset: Boolean = false) = dataKORDLOL.get(reset)
    suspend fun getKORDLOL(id: Int?) = getKORDLOL().find { it.id == id }
    suspend fun getLOL(id: Int?) = R2DBC.getLOLs { tbl_lols.id eq id }.firstOrNull()
    suspend fun getKORD(reset: Boolean = false) = dataKORD.get(reset)
    suspend fun getKORD(id: Int?) = getKORD().find { it.id == id }

    suspend fun getSavedParticipants() : ArrayList<statMainTemp_r2> {
        val arraySavedParticipants = ArrayList<statMainTemp_r2>()
        R2DBC.runQuery {
            QueryDsl.fromTemplate("SELECT * FROM get_player_stats_param(${guildSQL.id})").select {
                val id = it.int("id")?:0
                val games = it.int("games")?:0
                val win = it.int("win")?:0
                val kill = it.int("kill")?:0
                val kill2 = it.int("kill2")?:0
                val kill3 = it.int("kill3")?:0
                val kill4 = it.int("kill4")?:0
                val kill5 = it.int("kill5")?:0
                arraySavedParticipants.add(statMainTemp_r2(id, games, win, kill, kill2, kill3, kill4, kill5, null))
            }
        }
        arraySavedParticipants.forEach {
            it.kordLOL = getKORDLOL(it.kord_lol_id)
        }
        return arraySavedParticipants
    }

    suspend fun getArrayAramMMRData() : ArrayList<statAramDataTemp_r2> {
        val arrayAramMMRData = ArrayList<statAramDataTemp_r2>()
        R2DBC.runQuery {
            QueryDsl.fromTemplate("SELECT * FROM get_aram_data_param(${guildSQL.id})").select { row ->
                val id = row.int("id")
                val mmr_aram = row.double("mmr_aram")
                val mmr_aram_saved = row.double("mmr_aram_saved")
                val games = row.int("games")?:0
                val champion_id = row.int("champion_id")
                val mmr = row.double("mmr")
                val match_id = row.string("match_id")
                val mvp_lvp_info = row.string("mvp_lvp_info")
                val last_match_id = row.string("last_match_id")
                arrayAramMMRData.add(statAramDataTemp_r2(id!!, mmr_aram!!, mmr_aram_saved!!, games, champion_id, mmr, match_id, last_match_id, mvp_lvp_info,false, null))
            }
        }
        arrayAramMMRData.forEach {
            it.kordLOL = getKORDLOL(it.kord_lol_id)
        }
        return arrayAramMMRData
    }

    suspend fun onCalculateTimer() {
        Calc_Birthday(this, dataKORD.get()).calculate()
    }

    suspend fun addMatch(match: MatchDTO, mainOrder: Boolean) {
        val newMatch = Calc_AddMatch(this, match, getKORDLOL())
        newMatch.calculate(mainOrder)

        if (mainOrder) {
            val checkMatches = ArrayList<String>()
            val othersLOLS = newMatch.arrayOtherLOLs
            othersLOLS.forEach {
                if (it.LOL_puuid == "") return@forEach
                LeagueMainObject.catchMatchID(it.LOL_puuid, it.getCorrectName(), 0, 5).forEach ff@{ matchId ->
                    if (!checkMatches.contains(matchId)) checkMatches.add(matchId)
                }
            }
            val listChecked = getNewMatches(checkMatches)
            listChecked.sortBy { it }
            listChecked.forEach { newMatchs ->
                LeagueMainObject.catchMatch(newMatchs)?.let { match ->
                    addMatch(match, false)
                }
            }
        }

//        Calc_PentaSteal(this, match, newMatch).calculte()
    }

    suspend fun getWinStreak() : HashMap<Int, Int> {
        val mapWinStreak = HashMap<Int, Int>()
        R2DBC.runQuery {
            QueryDsl.fromTemplate("SELECT * FROM get_streak_results_param(${guildSQL.id})").select { row ->
                val pers = row.int("PERS")?:-1
                val res = row.int("RES")?:0
                val ZN = row.string("ZN")?:""
                when (ZN) {
                    "+" -> mapWinStreak[pers] = res
                    "-" -> mapWinStreak[pers] = -res
                    else -> Unit
                }
            }
        }
        return mapWinStreak
    }

    /**
     * Прогрузка матчей в базу
     * @param lols список объектов LOLs
     * @param count кол-во прогружаемых матчей по каждому объекту LOLs
     * @param mainOrder прогружать ли в базу так же матчи по игрокам из предыдущего матча (рекурсия 1го уровня)
     */
    suspend fun loadMatches(lols: List<LOLs>, count: Int, mainOrder: Boolean) {
        val checkMatches = ArrayList<String>()
        lols.forEach {
            if (it.LOL_puuid == "") return@forEach
            LeagueMainObject.catchMatchID(it.LOL_puuid, it.getCorrectName(), 0, count).forEach ff@{ matchId ->
                if (!checkMatches.contains(matchId)) checkMatches.add(matchId)
            }
        }
        val listChecked = getNewMatches(checkMatches)
        listChecked.sortBy { it }
        listChecked.forEach {newMatch ->
            LeagueMainObject.catchMatch(newMatch)?.let { match ->
                addMatch(match, mainOrder)
            }
        }
//        asyncLoadMatches(listChecked, mainOrder)
    }

    private suspend fun asyncLoadMatches(listChecked: ArrayList<String>, mainOrder: Boolean) {
        launch {
            if (listChecked.size > 2) {
                val list1 = listChecked.subList(0, listChecked.size / 2).toList()
                loadArray(list1, mainOrder)
                val list2 = listChecked.subList(listChecked.size / 2, listChecked.size).toList()
                loadArray(list2, mainOrder)
            } else {
                loadArray(listChecked, mainOrder)
            }
        }.join()
    }

    private suspend fun loadArray(listChecked: Collection<String>, mainOrder: Boolean) {
        asyncLaunch {
            listChecked.forEach {newMatch ->
                LeagueMainObject.catchMatch(newMatch)?.let { match ->
                    addMatch(match, mainOrder)
                }
            }
        }
    }

    suspend fun getNewMatches(list: ArrayList<String>): ArrayList<String> {
        list.removeAll(R2DBC.runQuery {
            QueryDsl.from(tbl_matches).where { tbl_matches.guild_id eq guildSQL.id }.select(tbl_matches.matchId)
        }.toSet())
        return list
    }
    suspend fun getMMRforChampion(championName: String) = dataMMR.get().find { it.champion == championName }
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SQLData_R2DBC

        if (guild != other.guild) return false
        if (guildSQL != other.guildSQL) return false
        if (isNeedUpdateDatas != other.isNeedUpdateDatas) return false
        if (dataKORDLOL != other.dataKORDLOL) return false
        if (dataKORD != other.dataKORD) return false
        if (dataMMR != other.dataMMR) return false
        if (dataSavedLOL != other.dataSavedLOL) return false
        if (dataSavedParticipants != other.dataSavedParticipants) return false

        return true
    }

    override fun hashCode(): Int {
        var result = guild.hashCode()
        result = 31 * result + guildSQL.hashCode()
        result = 31 * result + isNeedUpdateDatas.hashCode()
        result = 31 * result + dataKORDLOL.hashCode()
        result = 31 * result + dataKORD.hashCode()
        result = 31 * result + dataMMR.hashCode()
        result = 31 * result + dataSavedLOL.hashCode()
        result = 31 * result + dataSavedParticipants.hashCode()
        return result
    }
}