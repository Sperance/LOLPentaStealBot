package ru.descend.bot.postgre.calculating

import ru.descend.bot.datas.getData
import ru.descend.bot.datas.getDataOne
import ru.descend.bot.lowDescriptor
import ru.descend.bot.postgre.SQLData_R2DBC
import ru.descend.bot.postgre.r2dbc.model.KORDLOLs
import ru.descend.bot.postgre.r2dbc.model.KORDs
import ru.descend.bot.datas.update
import ru.descend.bot.generateAIText
import ru.descend.bot.postgre.r2dbc.model.LOLs
import ru.descend.bot.postgre.r2dbc.model.LOLs.Companion.tbl_lols
import ru.descend.bot.sendMessage
import ru.descend.bot.writeLog
import java.util.Calendar
import java.util.GregorianCalendar

data class Calc_Birthday(private var sqlData: SQLData_R2DBC, var dataList: List<KORDs>) {

    suspend fun calculate() {
        dataList.filter { isBirthday(it.date_birthday) }.forEach {
            var textMessage = ""
            if (textMessage.isEmpty()) textMessage = generateAIText("Напиши интересное с долей смеха поздравление пользователю ${it.asUser(sqlData.guild!!).lowDescriptor()} с Днём Рождения на сервере Discord посвященному игре Лиге Легенд")
            sqlData.sendMessage(sqlData.guildSQL.messageIdStatus, textMessage)
            writeLog(textMessage)
            val currentTextBirthday = it.date_birthday
            val newTextBirthday = currentTextBirthday.dropLast(4) + GregorianCalendar.getInstance().get(Calendar.YEAR)
            it.date_birthday = newTextBirthday
            it.update()
            val kordlol = KORDLOLs().getDataOne({ KORDLOLs.tbl_kordlols.KORD_id eq it.id })
            if (kordlol != null) {
                val listLols = LOLs().getData({ tbl_lols.id.eq(kordlol.LOL_id) })
                listLols.forEach { lol ->
                    lol.mmrAramSaved += (10.0 + lol.getARAMRank().rankValue)
                    lol.update()
                }
            }
        }
    }

    private fun isBirthday(dateValue: String) : Boolean {

        if (dateValue.length != 13) return false

        val valueDay = dateValue.substring(0..1).toInt()
        val valueMonth = dateValue.substring(2..3).toInt()
        val valueYear = dateValue.substring(4..7).toInt()
        val lastYear = dateValue.substring(9..12).toInt()

        val calendarInstance = GregorianCalendar.getInstance()
        val currentDay = calendarInstance.get(Calendar.DAY_OF_MONTH)
        val currentMonth = calendarInstance.get(Calendar.MONTH) + 1
        val currentYear = calendarInstance.get(Calendar.YEAR)

        return valueDay == currentDay && valueMonth == currentMonth && lastYear != currentYear
    }
}