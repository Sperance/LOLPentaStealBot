package ru.descend.bot.postgre

import dev.kord.core.entity.Guild
import org.komapper.core.dsl.QueryDsl
import org.komapper.core.dsl.query.double
import org.komapper.core.dsl.query.int
import org.komapper.core.dsl.query.string
import ru.descend.bot.lolapi.leaguedata.match_dto.MatchDTO
import ru.descend.bot.mail.GMailSender
import ru.descend.bot.postgre.calculating.Calc_AddMatch
import ru.descend.bot.postgre.calculating.Calc_PentaSteal
import ru.descend.bot.postgre.r2dbc.R2DBC
import ru.descend.bot.postgre.r2dbc.WorkData
import ru.descend.bot.postgre.r2dbc.model.Guilds
import ru.descend.bot.postgre.r2dbc.model.KORDLOLs
import ru.descend.bot.postgre.r2dbc.model.KORDs
import ru.descend.bot.postgre.r2dbc.model.LOLs
import ru.descend.bot.postgre.r2dbc.model.MMRs
import ru.descend.bot.postgre.r2dbc.model.Matches
import ru.descend.bot.postgre.r2dbc.model.Participants
import ru.descend.bot.postgre.r2dbc.model.tbl_KORDLOLs
import ru.descend.bot.postgre.r2dbc.model.tbl_KORDs
import ru.descend.bot.postgre.r2dbc.model.tbl_LOLs
import ru.descend.bot.postgre.r2dbc.model.tbl_matches
import ru.descend.bot.postgre.r2dbc.model.tbl_participants
import java.util.WeakHashMap

data class kordTemp_r2(var kordLOL: KORDLOLs, var lastParts: ArrayList<Participants>, var isBold: Boolean)
data class statMainTemp_r2(var kord_lol_id: Int, var games: Int, var win: Int, var kill: Int, var kill2: Int, var kill3: Int, var kill4: Int, var kill5: Int, var sortId: Int)
data class statAramDataTemp_r2(var kord_lol_id: Int, var mmr_aram: Double, var mmr_aram_saved: Double, var games: Int?, var champion_id: Int?, var mmr: Double?, var match_id: String?, var last_match_id: String?, var bold: Boolean, var sortId: Int)

class SQLData_R2DBC (var guild: Guild, var guildSQL: Guilds) {

    var listKordTemp = ArrayList<kordTemp_r2>()
    var listMainData = ArrayList<KORDLOLs>()

    val dataGuild = WorkData<Guilds>()
    val dataKORDLOL = WorkData<KORDLOLs>()
    val dataKORD = WorkData<KORDs>()
    val dataLOL = WorkData<LOLs>()
    val dataMatches = WorkData<Matches>()
    val dataMMR = WorkData<MMRs>()

    val dataSavedLOL = WorkData<LOLs>()
    val dataSavedParticipants = WorkData<Participants>()

    fun initialize() {
        if (dataGuild.bodyReset == null) dataGuild.bodyReset = { R2DBC.getGuilds(null) }
        if (dataKORDLOL.bodyReset == null) dataKORDLOL.bodyReset = { R2DBC.getKORDLOLs { tbl_KORDLOLs.guild_id eq guildSQL.id } }
        if (dataKORD.bodyReset == null) dataKORD.bodyReset = { R2DBC.getKORDs { tbl_KORDs.guild_id eq guildSQL.id } }
        if (dataLOL.bodyReset == null) dataLOL.bodyReset = { R2DBC.getLOLs(null) }
        if (dataMatches.bodyReset == null) dataMatches.bodyReset = { R2DBC.getMatches { tbl_matches.guild_id eq guildSQL.id } }
        if (dataMMR.bodyReset == null) dataMMR.bodyReset = { R2DBC.getMMRs(null) }

        if (dataSavedLOL.bodyReset == null) {
            dataSavedLOL.bodyReset = {
                val kordLol_lol_id = dataKORDLOL.get().map { it.LOL_id }
                R2DBC.runQuery(QueryDsl.from(tbl_LOLs).where { tbl_LOLs.id.inList(kordLol_lol_id) })
            }
        }

        if (dataSavedParticipants.bodyReset == null) {
            dataSavedParticipants.bodyReset = {
                val kordLol_lol_id = dataKORDLOL.get().map { it.LOL_id }
                R2DBC.runQuery(QueryDsl.from(tbl_participants).where { tbl_participants.LOLperson_id.inList(kordLol_lol_id) })
            }
        }
    }

    fun performClear() {
        dataGuild.clear()
        dataKORDLOL.clear()
        dataKORD.clear()
        dataLOL.clear()
        dataMatches.clear()
        dataMMR.clear()

        dataSavedLOL.clear()
        dataSavedParticipants.clear()
    }

    /*-----*/
    suspend fun getKORDLOL(reset: Boolean = false) = dataKORDLOL.get(reset)
    suspend fun getKORDLOL(id: Int) = getKORDLOL().find { it.id == id }
    /*-----*/
    suspend fun getLOL(reset: Boolean = false) = dataLOL.get(reset)
    suspend fun getLOL(id: Int) = getLOL().find { it.id == id }
    /*-----*/
    suspend fun getKORD(reset: Boolean = false) = dataKORD.get(reset)
    suspend fun getKORD(id: Int) = getKORD().find { it.id == id }

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
                arraySavedParticipants.add(statMainTemp_r2(id, games, win, kill, kill2, kill3, kill4, kill5, -1))
            }
        }
        arraySavedParticipants.forEach {
            it.sortId = getKORDLOL(it.kord_lol_id)?.showCode?:-1
        }
        arraySavedParticipants.sortBy { it.sortId }
        return arraySavedParticipants
    }

    suspend fun getArrayAramMMRData() : ArrayList<statAramDataTemp_r2> {
        val arrayAramMMRData = ArrayList<statAramDataTemp_r2>()
        R2DBC.runQuery {
            QueryDsl.fromTemplate("SELECT * FROM get_aram_data_param(${guildSQL.id})").select { row ->
                val id = row.int("id")
                val mmr_aram = row.double("mmr_aram")
                val mmr_aram_saved = row.double("mmr_aram_saved")
                val games = row.int("games")
                val champion_id = row.int("champion_id")
                val mmr = row.double("mmr")
                val match_id = row.string("match_id")
                val last_match_id = row.string("last_match_id")
                arrayAramMMRData.add(statAramDataTemp_r2(id!!, mmr_aram!!, mmr_aram_saved!!, games, champion_id, mmr, match_id, last_match_id, false, -1))
            }
        }
        arrayAramMMRData.forEach {
            it.sortId = getKORDLOL(it.kord_lol_id)?.showCode?:-1
        }
        arrayAramMMRData.sortBy { it.sortId }
        return arrayAramMMRData
    }

    suspend fun addMatch(match: MatchDTO) {
        val newMatch = Calc_AddMatch(this, match, getKORDLOL()).calculate()
        Calc_PentaSteal(this, match, newMatch).calculte()
    }

    suspend fun getWinStreak() : WeakHashMap<Int, Int> {
        var mapWinStreak = WeakHashMap<Int, Int>()
        R2DBC.runQuery {
            QueryDsl.fromTemplate("SELECT * FROM get_streak_results_param(${guildSQL.id})").select { row ->
                val pers = row.int("PERS")
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

    suspend fun getNewMatches(list: ArrayList<String>): ArrayList<String> {
        dataMatches.get(true).forEach { list.remove(it.matchId) }
        return list
    }

    suspend fun getSavedParticipantsForMatch(matchId: Int) = dataSavedParticipants.get(true).filter { it.match_id == matchId }
    suspend fun getParticipantsForMatch(matchId: Int) = R2DBC.getParticipants { tbl_participants.guild_id eq guildSQL.id ; tbl_participants.match_id eq matchId }
    suspend fun getMMRforChampion(championName: String) = dataMMR.get().find { it.champion == championName }

    fun sendEmail(theme: String, message: String) {
        try {
            GMailSender("llps.sys.bot@gmail.com", "esjk bphc hsjh otcx")
                .sendMail(
                    "[${guildSQL.name}] $theme",
                    message,
                    "llps.sys.bot@gmail.com",
                    "kaltemeis@gmail.com"
                )
        }catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun executeProcedure(body: String) {
        QueryDsl.executeScript(body)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SQLData_R2DBC

        if (guild != other.guild) return false
        if (guildSQL != other.guildSQL) return false
        if (listKordTemp != other.listKordTemp) return false
        if (listMainData != other.listMainData) return false
        if (dataGuild != other.dataGuild) return false
        if (dataKORDLOL != other.dataKORDLOL) return false
        if (dataKORD != other.dataKORD) return false
        if (dataLOL != other.dataLOL) return false
        if (dataMatches != other.dataMatches) return false
        if (dataMMR != other.dataMMR) return false
        if (dataSavedLOL != other.dataSavedLOL) return false
        if (dataSavedParticipants != other.dataSavedParticipants) return false

        return true
    }

    override fun hashCode(): Int {
        var result = guild.hashCode()
        result = 31 * result + guildSQL.hashCode()
        result = 31 * result + listKordTemp.hashCode()
        result = 31 * result + listMainData.hashCode()
        result = 31 * result + dataGuild.hashCode()
        result = 31 * result + dataKORDLOL.hashCode()
        result = 31 * result + dataKORD.hashCode()
        result = 31 * result + dataLOL.hashCode()
        result = 31 * result + dataMatches.hashCode()
        result = 31 * result + dataMMR.hashCode()
        result = 31 * result + dataSavedLOL.hashCode()
        result = 31 * result + dataSavedParticipants.hashCode()
        return result
    }
}