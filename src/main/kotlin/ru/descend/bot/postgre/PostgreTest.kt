package ru.descend.bot.postgre

import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.komapper.core.dsl.QueryDsl
import org.komapper.core.dsl.metamodel.EntityMetamodel
import org.komapper.core.dsl.metamodel.PropertyMetamodel
import ru.descend.bot.postgre.r2dbc.R2DBC
import ru.descend.bot.postgre.r2dbc.create
import ru.descend.bot.postgre.r2dbc.delete
import ru.descend.bot.postgre.r2dbc.getField
import ru.descend.bot.postgre.r2dbc.model.MMRs
import ru.descend.bot.postgre.r2dbc.update
import ru.descend.bot.printLog
import ru.descend.bot.savedObj.EnumMMRRank
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.createInstance

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
            val mmr = MMRs(champion = "champ").create(MMRs::champion)
            printLog(mmr)
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