package ru.descend.bot.postgre

import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.komapper.core.dsl.QueryDsl
import org.komapper.core.dsl.options.InsertOptions
import org.komapper.core.dsl.query.boolean
import org.komapper.core.dsl.query.double
import org.komapper.core.dsl.query.int
import org.komapper.core.dsl.query.long
import org.komapper.core.dsl.query.string
import ru.descend.bot.catchToken
import ru.descend.bot.lolapi.LeagueApi
import ru.descend.bot.lolapi.dataclasses.SavedPartSteal
import ru.descend.bot.postgre.r2dbc.R2DBC
import ru.descend.bot.postgre.r2dbc.model.KORDLOLs
import ru.descend.bot.postgre.r2dbc.model.KORDs
import ru.descend.bot.postgre.r2dbc.model.LOLs
import ru.descend.bot.postgre.r2dbc.model.Matches
import ru.descend.bot.postgre.r2dbc.model.Participants
import ru.descend.bot.postgre.r2dbc.model.tbl_KORDLOLs
import ru.descend.bot.postgre.r2dbc.model.tbl_KORDs
import ru.descend.bot.postgre.r2dbc.model.tbl_LOLs
import ru.descend.bot.postgre.r2dbc.model.tbl_MMRs
import ru.descend.bot.postgre.r2dbc.model.tbl_matches
import ru.descend.bot.postgre.r2dbc.model.tbl_participants
import ru.descend.bot.printLog
import ru.descend.bot.savedObj.EnumMMRRank
import ru.descend.bot.savedObj.getStrongDate
import ru.descend.bot.toDate
import java.time.Duration

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

    @Test
    fun test_mmr() {
        runBlocking {
            printLog(R2DBC.getMMRs { tbl_MMRs.id eq 4 })
        }
    }

    @Test
    fun checkMatchContains() {
        EnumMMRRank.entries.forEach {
            printLog("${it.nameRank} - ${it.ordinal} - ${((it.ordinal / 10.0) * 2.0) + 1.0}")
        }
    }
}