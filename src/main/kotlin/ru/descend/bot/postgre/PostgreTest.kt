package ru.descend.bot.postgre

import kotlinx.coroutines.runBlocking
import kotlinx.datetime.toKotlinLocalDate
import org.junit.Test
import ru.descend.bot.enums.EnumMMRRank
import ru.descend.bot.postgre.r2dbc.create
import ru.descend.bot.postgre.r2dbc.model.MMRs
import ru.descend.bot.postgre.r2dbc.update
import ru.descend.bot.printLog
import ru.descend.bot.savedObj.getDate
import ru.descend.bot.savedObj.getStrongDate
import ru.descend.bot.savedObj.toDate
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.GregorianCalendar

class PostgreTest {

    @Test
    fun test_mmrs(){
        printLog("MMR: ${EnumMMRRank.getMMRRank(42.0)}")
        printLog("MMR: ${EnumMMRRank.getMMRRank(420.0)}")
        printLog("MMR: ${EnumMMRRank.getMMRRank(0.0)}")
        printLog("MMR: ${EnumMMRRank.getMMRRank(89.5)}")
    }

    @Test
    fun testMethod() {
        printLog(1)
        val listIds = ArrayList<String>()
        listIds.add("RU_476092238")
        listIds.add("RE_476370823")
        listIds.add("RU_476367408")
    }

    fun isNeedSetLowerYear(dateValue: String) : Boolean {
        val valueDay = dateValue.substring(0..1).toInt()
        val valueMonth = dateValue.substring(2..3).toInt()
        val curDate = LocalDate.of(2000, valueMonth, valueDay)
        val curSysDate = LocalDate.of(2000, LocalDate.now().monthValue, LocalDate.now().dayOfMonth)
        if (curDate < curSysDate) return false
        if (curDate > curSysDate) return true
        if (curDate == curSysDate) return true
        return false
    }

    @Test
    fun test_birthday_parse() {
        val dateValue = "05041900_2024"

        val valueDay = dateValue.substring(0..1).toInt()
        val valueMonth = dateValue.substring(2..3).toInt()
        val valueYear = dateValue.substring(4..7).toInt()
        printLog("d$valueDay m$valueMonth y$valueYear")

        val curDate = LocalDate.of(2000, valueMonth, valueDay)
        printLog(curDate.dayOfMonth)
        printLog(curDate.monthValue)
        printLog(curDate.year)

        val curSysDate = LocalDate.of(2000, LocalDate.now().monthValue, LocalDate.now().dayOfMonth)
        if (curDate < curSysDate) printLog("low")
        if (curDate > curSysDate) printLog("great")
        if (curDate == curSysDate) printLog("eq")
    }

    @Test
    fun test_mmr() {
        runBlocking {
            val mmr = MMRs(champion = "champ")

            mmr.create(MMRs::champion)
            printLog(mmr)
            val str = "asd"
            mmr.champion = "champ new"
            mmr.update()
            printLog(mmr)
        }
    }

    @Test
    fun checkMatchContains() {
        EnumMMRRank.entries.forEach {
            printLog("${it.nameRank} - ${it.ordinal} - ${((it.ordinal / 10.0) * 2.0) + 1.0}")
        }
    }
}