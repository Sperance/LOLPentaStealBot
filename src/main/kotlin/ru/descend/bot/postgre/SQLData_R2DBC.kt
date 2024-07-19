package ru.descend.bot.postgre

import dev.kord.core.entity.Guild
import org.komapper.core.dsl.QueryDsl
import org.komapper.core.dsl.query.double
import org.komapper.core.dsl.query.get
import org.komapper.core.dsl.query.int
import org.komapper.core.dsl.query.string
import ru.descend.bot.DAYS_MIN_IN_LOAD
import ru.descend.bot.EnumMeasures
import ru.descend.bot.LOAD_MATCHES_ON_SAVED_UNDEFINED
import ru.descend.bot.datas.TextDicrordLimit
import ru.descend.bot.lolapi.LeagueMainObject
import ru.descend.bot.postgre.calculating.Calc_AddMatch
import ru.descend.bot.postgre.calculating.Calc_Birthday
import ru.descend.bot.datas.WorkData
import ru.descend.bot.datas.getData
import ru.descend.bot.datas.getDataOne
import ru.descend.bot.datas.toDate
import ru.descend.bot.datas.toLocalDate
import ru.descend.bot.generateAIText
import ru.descend.bot.lolapi.dto.matchDto.MatchDTO
import ru.descend.bot.measureBlock
import ru.descend.bot.postgre.r2dbc.model.Guilds
import ru.descend.bot.postgre.r2dbc.model.KORDLOLs
import ru.descend.bot.postgre.r2dbc.model.KORDLOLs.Companion.tbl_kordlols
import ru.descend.bot.postgre.r2dbc.model.KORDs
import ru.descend.bot.postgre.r2dbc.model.KORDs.Companion.tbl_kords
import ru.descend.bot.postgre.r2dbc.model.LOLs
import ru.descend.bot.postgre.r2dbc.model.LOLs.Companion.tbl_lols
import ru.descend.bot.postgre.r2dbc.model.MMRs
import ru.descend.bot.postgre.r2dbc.model.ParticipantsNew
import ru.descend.bot.postgre.r2dbc.model.ParticipantsNew.Companion.tbl_participantsnew
import ru.descend.bot.sendMessage
import java.util.Date
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

data class statMainTemp_r2(var kord_lol_id: Int, var games: Int, var win: Int, var kill: Int, var kill2: Int, var kill3: Int, var kill4: Int, var kill5: Int, var kordLOL: KORDLOLs?)
data class statAramDataTemp_r2(var kord_lol_id: Int, var mmr_aram: Double, var mmr_aram_saved: Double, var champion_id: Int?, var mmr: Double?, var mvp_lvp_info: String?, var bold: Boolean, var kordLOL: KORDLOLs?)

class SQLData_R2DBC (var guild: Guild, var guildSQL: Guilds) {

    var isNeedUpdateDays = false
    var updatesBeforeLoadUsersMatch = 0
    var textNewMatches = TextDicrordLimit()
    var olderDateLong = Date().time.toLocalDate().minusDays(DAYS_MIN_IN_LOAD).toDate().time
    var currentDateLong = Date().time
    val atomicIntLoaded = AtomicInteger()
    val atomicNeedUpdateTables = AtomicBoolean(true)

    val dataKORDLOL = WorkData<KORDLOLs>("KORDLOL")
    val dataKORD = WorkData<KORDs>("KORD")

    val dataSavedLOL = WorkData<LOLs>("SavedLOL")

    fun initialize() {
        if (dataKORDLOL.bodyReset == null) dataKORDLOL.bodyReset = { KORDLOLs().getData({ tbl_kordlols.guild_id eq guildSQL.id }) }
        if (dataKORD.bodyReset == null) dataKORD.bodyReset = { KORDs().getData({ tbl_kords.guild_id eq guildSQL.id }) }

        if (dataSavedLOL.bodyReset == null) {
            dataSavedLOL.bodyReset = {
                val kordLol_lol_id = dataKORDLOL.get().map { it.LOL_id }
                R2DBC.runQuery(QueryDsl.from(tbl_lols).where { tbl_lols.id.inList(kordLol_lol_id) })
            }
        }
    }

    fun clearTempData() {
        textNewMatches.clear()
        atomicIntLoaded.set(0)
        olderDateLong = Date().time.toLocalDate().minusDays(DAYS_MIN_IN_LOAD).toDate().time
        currentDateLong = Date().time
    }

    suspend fun getKORDLOL(reset: Boolean = false) = dataKORDLOL.get(reset)
    suspend fun getKORDLOL(id: Int?) = getKORDLOL().find { it.id == id }
    suspend fun getKORD(reset: Boolean = false) = dataKORD.get(reset)
    suspend fun getKORD(id: Int?) = getKORD().find { it.id == id }
    suspend fun getKORDLOL_fromLOL(lolid: Int) = getKORDLOL().find { it.LOL_id == lolid }

    suspend fun getSavedParticipants() : ArrayList<statMainTemp_r2> {
        val arraySavedParticipants = ArrayList<statMainTemp_r2>()
        measureBlock(EnumMeasures.QUERY, "get_player_stats_param") {
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
        }
        return arraySavedParticipants
    }

    suspend fun getArrayAramMMRData() : ArrayList<statAramDataTemp_r2> {
        val arrayAramMMRData = ArrayList<statAramDataTemp_r2>()
        measureBlock(EnumMeasures.QUERY, "get_aram_data_param") {
            R2DBC.runQuery {
                QueryDsl.fromTemplate("SELECT * FROM get_aram_data_param(${guildSQL.id})").select { row ->
                    val id = row.int("id")
                    val mmr_aram = row.double("mmr_aram")
                    val mmr_aram_saved = row.double("mmr_aram_saved")
                    val champion_id = row.int("champion_id")
                    val mmr = row.double("mmr")
                    val mvp_lvp_info = row.string("mvp_lvp_info")
                    val last_game = row.string("last_game")
                    arrayAramMMRData.add(statAramDataTemp_r2(id!!, mmr_aram!!, mmr_aram_saved!!, champion_id, mmr, mvp_lvp_info,last_game == "+", null))
                }
            }
            arrayAramMMRData.forEach {
                it.kordLOL = getKORDLOL_fromLOL(it.kord_lol_id)
            }
        }
        return arrayAramMMRData
    }

    suspend fun getSavedLOL(lol: LOLs?) : KORDLOLs? {
        if (lol == null) return null
        return dataKORDLOL.get().find { it.LOL_id == lol.id }
    }

    suspend fun onCalculateTimer() {
        Calc_Birthday(this, dataKORD.get()).calculate()
    }

    private suspend fun addMatch(match: MatchDTO, mainOrder: Boolean) {
        val newMatch = Calc_AddMatch(this, match)
        newMatch.calculate()
        if (mainOrder) {
            val othersLOLS = newMatch.arrayOtherLOLs
            loadMatches(othersLOLS, LOAD_MATCHES_ON_SAVED_UNDEFINED, false)
        }
    }

    suspend fun getWinStreak() : HashMap<Int, Int> {
        val tempMapWinStreak = HashMap<Int, Int>()
        measureBlock(EnumMeasures.QUERY, "get_streak_results_param") {
            R2DBC.runQuery {
                QueryDsl.fromTemplate("SELECT * FROM get_streak_results_param(${guildSQL.id})").select { row ->
                    val pers = row.int("PERS")?:-1
                    val res = row.int("RES")?:0
                    val ZN = row.string("ZN")?:""
                    when (ZN) {
                        "+" -> tempMapWinStreak[pers] = res
                        "-" -> tempMapWinStreak[pers] = -res
                        else -> Unit
                    }
                }
            }
        }
        return tempMapWinStreak
    }

    suspend fun loadMatches(lols: Collection<LOLs>, count: Int, mainOrder: Boolean) {
        val checkMatches = ArrayList<String>()
        lols.forEach {
            if (it.LOL_puuid == "") return@forEach
            atomicIntLoaded.incrementAndGet()
            LeagueMainObject.catchMatchID(it, 0, count).forEach ff@{ matchId ->
                if (!checkMatches.contains(matchId)) checkMatches.add(matchId)
            }
        }
        loadArrayMatches(checkMatches, mainOrder)
    }

    suspend fun loadArrayMatches(checkMatches: ArrayList<String>, mainOrder: Boolean) {
        val listChecked = getNewMatches(checkMatches)
        listChecked.sortBy { it }
        listChecked.forEach { newMatch ->
            atomicIntLoaded.incrementAndGet()
            if (mainOrder && !atomicNeedUpdateTables.get()) {
                atomicNeedUpdateTables.set(true)
            }
            R2DBC.runTransaction {
                LeagueMainObject.catchMatch(newMatch)?.let { match ->
                    addMatch(match, mainOrder)
                }
            }
        }
    }

    suspend fun getNewMatches(list: ArrayList<String>): ArrayList<String> {
        var resultAra = list
        val dataAra = resultAra.joinToString(prefix = "{", postfix = "}")
        val sql = "SELECT remove_matches('$dataAra'::character varying[])"
        R2DBC.runQuery {
            QueryDsl.fromTemplate(sql).select {
                val data = it.get<Array<String>>(0)
                if (data == null) resultAra.clear()
                else resultAra.removeAll(data.toSet())
            }
        }
        return resultAra
    }

    suspend fun generateFact() {
        val generatedText = generateAIText("Напиши интересный факт про игру League of Legends")
        val resultedText = "**Рубрика: интересные факты**\n\n$generatedText"
        sendMessage(guildSQL.messageIdStatus, resultedText)
    }
}