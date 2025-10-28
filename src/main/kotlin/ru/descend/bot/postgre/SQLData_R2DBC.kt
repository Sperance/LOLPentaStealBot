package ru.descend.bot.postgre

import dev.kord.core.entity.Guild
import org.komapper.core.dsl.QueryDsl
import org.komapper.core.dsl.query.double
import org.komapper.core.dsl.query.int
import org.komapper.core.dsl.query.string
import ru.descend.bot.EnumMeasures
import ru.descend.bot.postgre.calculating.Calc_Birthday
import ru.descend.bot.datas.WorkData
import ru.descend.bot.datas.getData
import ru.descend.bot.datas.getStrongDate
import ru.descend.bot.datas.isCurrentDay
import ru.descend.bot.generateAIText
import ru.descend.bot.launch
import ru.descend.bot.lowDescriptor
import ru.descend.bot.measureBlock
import ru.descend.bot.postgre.r2dbc.model.Guilds
import ru.descend.bot.postgre.r2dbc.model.KORDLOLs
import ru.descend.bot.postgre.r2dbc.model.KORDs
import ru.descend.bot.postgre.r2dbc.model.LOLs
import ru.descend.bot.postgre.r2dbc.model.LOLs.Companion.tbl_lols
import ru.descend.bot.postgre.r2dbc.model.Matches
import ru.descend.bot.postgre.r2dbc.model.ParticipantsNew
import ru.descend.bot.printLog
import ru.descend.bot.sendMessage
import ru.descend.bot.sqlData
import ru.descend.bot.toDate
import ru.descend.bot.toFormatDateTime

data class statAramDataTemp_r2(var kord_lol_id: Int, var mmr_aram: Double, var mmr_aram_saved: Double, var champion_id: Int?, var mmr: Double?, var mvp_lvp_info: String?, var bold: Boolean, var kordLOL: KORDLOLs?, var LOL: LOLs?)

class SQLData_R2DBC(var guild: Guild, var guildSQL: Guilds) {

    var isHaveLastARAM = true
    var isNeedUpdateDays = false

    val dataKORDLOL = WorkData<KORDLOLs>("KORDLOL")
    val dataKORD = WorkData<KORDs>("KORD")
    val dataSavedLOL = WorkData<LOLs>("SavedLOL")

    fun initialize() {
        if (dataKORDLOL.bodyReset == null) dataKORDLOL.bodyReset = { KORDLOLs().getData() }
        if (dataKORD.bodyReset == null) dataKORD.bodyReset = { KORDs().getData() }
        if (dataSavedLOL.bodyReset == null) dataSavedLOL.bodyReset = { LOLs().getData({ tbl_lols.show_code notEq 0 }) }
    }

    suspend fun getKORD(id: Int?) = dataKORD.get().find { it.id == id }
    private suspend fun getKORDLOL_fromLOL(lolid: Int) = dataKORDLOL.get().find { it.LOL_id == lolid }

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
            val generatedText = generateAIText("Напиши длинное прикольное поздравление в шуточном стиле с извратом пользователю ${curLol.asUser(this@SQLData_R2DBC).lowDescriptor()} за то, что он сделал Пентакилл в игре League of Legends за чемпиона $championName в режиме ARAM")
            val resultText = "Поздравляем!!!\n${curLol.asUser(this@SQLData_R2DBC).lowDescriptor()} cделал Пентакилл$textPentasCount за $championName\nМатч: ${match.matchId} Дата: ${match.matchDateEnd.toFormatDateTime()}\n\n$generatedText"
            sendMessage(guildSQL.messageIdStatus, resultText)
        }
    }

    suspend fun getArrayAramMMRData() : ArrayList<statAramDataTemp_r2> {
        printLog("[getArrayAramMMRData] 1")
        val arrayAramMMRData = ArrayList<statAramDataTemp_r2>()
        measureBlock(EnumMeasures.QUERY, "get_aram_data_param") {
            printLog("[getArrayAramMMRData] 2")
            R2DBC.runQuery {
                printLog("[getArrayAramMMRData] 3")
                QueryDsl.fromTemplate("SELECT * FROM get_aram_data_param()").select { row ->
                    val id = row.int("id")
                    val mmr_aram = row.double("mmr_aram")
                    val mmr_aram_saved = row.double("mmr_aram_saved")
                    val champion_id = row.int("champion_id")
                    val mmr = row.double("mmr")
                    val mvp_lvp_info = row.string("mvp_lvp_info")
                    val last_game = row.string("last_game")
                    arrayAramMMRData.add(statAramDataTemp_r2(id!!, mmr_aram!!, mmr_aram_saved!!, champion_id, mmr, mvp_lvp_info,last_game == "+", null, null))
                }
            }
            printLog("[getArrayAramMMRData] 4")
            val lols = sqlData?.dataSavedLOL?.get() ?: LOLs().getData({ tbl_lols.show_code notEq 0 })
            arrayAramMMRData.forEach {
                it.kordLOL = getKORDLOL_fromLOL(it.kord_lol_id)
                it.LOL = lols.find { ll -> ll.id == it.kordLOL?.LOL_id }
            }
            printLog("[getArrayAramMMRData] 5")
        }
        printLog("[getArrayAramMMRData] 6")
        return arrayAramMMRData
    }

    suspend fun onCalculateTimer() {
        Calc_Birthday(this, dataKORD.get()).calculate()
    }
}