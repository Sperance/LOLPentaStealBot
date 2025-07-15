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
import ru.descend.bot.datas.getStrongDate
import ru.descend.bot.datas.isCurrentDay
import ru.descend.bot.datas.toDate
import ru.descend.bot.datas.toLocalDate
import ru.descend.bot.generateAIText
import ru.descend.bot.launch
import ru.descend.bot.lolapi.dto.matchDto.MatchDTO
import ru.descend.bot.lowDescriptor
import ru.descend.bot.measureBlock
import ru.descend.bot.postgre.r2dbc.model.Guilds
import ru.descend.bot.postgre.r2dbc.model.Heroes
import ru.descend.bot.postgre.r2dbc.model.KORDLOLs
import ru.descend.bot.postgre.r2dbc.model.KORDLOLs.Companion.tbl_kordlols
import ru.descend.bot.postgre.r2dbc.model.KORDs
import ru.descend.bot.postgre.r2dbc.model.KORDs.Companion.tbl_kords
import ru.descend.bot.postgre.r2dbc.model.LOLs
import ru.descend.bot.postgre.r2dbc.model.LOLs.Companion.tbl_lols
import ru.descend.bot.postgre.r2dbc.model.Matches
import ru.descend.bot.postgre.r2dbc.model.Matches.Companion.tbl_matches
import ru.descend.bot.postgre.r2dbc.model.ParticipantsNew
import ru.descend.bot.printLog
import ru.descend.bot.sendMessage
import ru.descend.bot.toDate
import ru.descend.bot.toFormatDateTime
import java.util.Date
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

data class statMainTemp_r2(var kord_lol_id: Int, var games: Int, var win: Int, var kill: Int, var kill2: Int, var kill3: Int, var kill4: Int, var kill5: Int, var kordLOL: KORDLOLs?)
data class statAramDataTemp_r2(var kord_lol_id: Int, var mmr_aram: Double, var mmr_aram_saved: Double, var champion_id: Int?, var mmr: Double?, var mvp_lvp_info: String?, var bold: Boolean, var kordLOL: KORDLOLs?)

class SQLData_R2DBC (var guild: Guild, var guildSQL: Guilds) {

    var isHaveLastARAM = false
    var isNeedUpdateDays = false

    val dataKORDLOL = WorkData<KORDLOLs>("KORDLOL")
    val dataKORD = WorkData<KORDs>("KORD")
    val dataSavedLOL = WorkData<LOLs>("SavedLOL")

    fun initialize() {
        if (dataKORDLOL.bodyReset == null) dataKORDLOL.bodyReset = { KORDLOLs().getData() }
        if (dataKORD.bodyReset == null) dataKORD.bodyReset = { KORDs().getData() }

        if (dataSavedLOL.bodyReset == null) {
            dataSavedLOL.bodyReset = {
                val kordLol_lol_id = dataKORDLOL.get().map { it.LOL_id }
                R2DBC.runQuery(QueryDsl.from(tbl_lols).where { tbl_lols.id.inList(kordLol_lol_id) })
            }
        }
    }

    suspend fun getKORDLOL(reset: Boolean = false) = dataKORDLOL.get(reset)
    private suspend fun getKORDLOL(id: Int?) = getKORDLOL().find { it.id == id }
    private suspend fun getKORD(reset: Boolean = false) = dataKORD.get(reset)
    suspend fun getKORD(id: Int?) = getKORD().find { it.id == id }
    private suspend fun getKORDLOL_fromLOL(lolid: Int) = getKORDLOL().find { it.LOL_id == lolid }

    suspend fun getSavedParticipants() : ArrayList<statMainTemp_r2> {
        val arraySavedParticipants = ArrayList<statMainTemp_r2>()
        measureBlock(EnumMeasures.QUERY, "get_player_stats_param") {
            R2DBC.runQuery {
                QueryDsl.fromTemplate("SELECT * FROM get_player_stats_param()").select {
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

    fun calculatePentakill(lol: LOLs, part: ParticipantsNew, match: Matches) {
        if (part.kills5 <= 0) return
        printLog("[calculatePentakill] lol: $lol part: $part")
        if (!match.matchDateEnd.toDate().isCurrentDay()) {
            printLog("[calculatePentakill] lol: $lol part: $part - matchDate(${match.matchDateEnd.toDate().getStrongDate().date}) is not current date(${System.currentTimeMillis().toDate().getStrongDate().date})")
            return
        }
        launch {
            val curLol = getKORDLOL_fromLOL(lol.id)
            if (curLol == null) {
                printLog("KORDLOL does not exists: id ${lol.id} puuid ${lol.LOL_puuid}")
                return@launch
            }
            val championName = part.getHeroNameRU()
            val textPentasCount = if (part.kills5 == 1) "" else "(${part.kills5})"
            val generatedText = generateAIText("Напиши прикольное поздравление в шуточном стиле пользователю ${curLol.asUser(this@SQLData_R2DBC).lowDescriptor()} за то, что он сделал Пентакилл в игре League of Legends за чемпиона $championName в режиме ${match.matchMode}")
            val resultText = "Поздравляем!!!\n${curLol.asUser(this@SQLData_R2DBC).lowDescriptor()} cделал Пентакилл$textPentasCount за $championName\nМатч: ${match.matchId} Дата: ${match.matchDateEnd.toFormatDateTime()}\n\n$generatedText"
            sendMessage(guildSQL.messageIdStatus, resultText)
        }
    }

    suspend fun getArrayAramMMRData() : ArrayList<statAramDataTemp_r2> {
        val arrayAramMMRData = ArrayList<statAramDataTemp_r2>()
        measureBlock(EnumMeasures.QUERY, "get_aram_data_param") {
            R2DBC.runQuery {
                QueryDsl.fromTemplate("SELECT * FROM get_aram_data_param()").select { row ->
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

    suspend fun onCalculateTimer() {
        Calc_Birthday(this, dataKORD.get()).calculate()
    }
}