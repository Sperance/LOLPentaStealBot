package ru.descend.bot.postgre

import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.komapper.core.dsl.QueryDsl
import org.komapper.core.dsl.metamodel.EntityMetamodel
import org.komapper.core.dsl.metamodel.PropertyMetamodel
import org.komapper.core.dsl.metamodel.getAutoIncrementProperty
import ru.descend.bot.postgre.r2dbc.R2DBC
import ru.descend.bot.postgre.r2dbc.model.MMRs
import ru.descend.bot.postgre.r2dbc.model.tbl_KORDLOLs
import ru.descend.bot.postgre.r2dbc.model.tbl_MMRs
import ru.descend.bot.printLog
import ru.descend.bot.savedObj.EnumMMRRank
import kotlin.reflect.KProperty1
import kotlin.reflect.javaType

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
            tbl_MMRs.selectId()
            val res = tbl_MMRs.save(MMRs(champion = "sample"), MMRs::id)
            printLog("res: $res")
        }
    }

    fun Any.getField(name: String) = this::class.java.declaredFields.find { it.isAccessible = true ; it.name == name }?.get(this)

    @Suppress("UNCHECKED_CAST")
    suspend fun <ENTITY : Any, ID : Any, META : EntityMetamodel<ENTITY, ID, META>> META.save(
        obj: ENTITY,
        kProperty1: KProperty1<ENTITY, *>
    ) : ENTITY {
        val metaProperty = this.properties().find { it.name == kProperty1.name } as PropertyMetamodel<ENTITY, *, *>
        val already = R2DBC.runQuery {
            QueryDsl.from(this@save)
//                .where { metaProperty eq kProperty1.get(obj) }
                .limit(1)
                .select(this@save.getAutoIncrementProperty() as PropertyMetamodel<ENTITY, Int, Int>)
        }.isNotEmpty()
        return R2DBC.runQuery { QueryDsl.insert(this@save).single(obj) }
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun <ENTITY : Any, ID : Any, META : EntityMetamodel<ENTITY, ID, META>> META.delete(obj: ENTITY) {
        R2DBC.runQuery {
            val prop_id = this@delete.getAutoIncrementProperty() as PropertyMetamodel<ENTITY, Int, Int>
            QueryDsl.delete(this@delete).where { prop_id eq obj.getField("id") as Int }
        }
    }

    suspend fun <ENTITY : Any, ID : Any, META : EntityMetamodel<ENTITY, ID, META>> META.selectId() {
        val res = R2DBC.runQuery {
            val prop_id = this@selectId.getAutoIncrementProperty() as PropertyMetamodel<ENTITY, Int, Int>
            QueryDsl.from(this@selectId).where { prop_id eq 1 }
        }
        printLog(res)
    }

    suspend fun <ENTITY : Any, ID : Any, META : EntityMetamodel<ENTITY, ID, META>> save(metamodel: META) {
        var moedlId: PropertyMetamodel<ENTITY, Int, Int>? = null
//        metamodel.properties().forEach {
//            if (it.name == "id") moedlId = it as PropertyMetamodel<ENTITY, Int, Int>
//        }
        moedlId = metamodel.getAutoIncrementProperty() as PropertyMetamodel<ENTITY, Int, Int>?
        val res = R2DBC.runQuery {
            val pr3 = moedlId
            QueryDsl.delete(metamodel).where { pr3!! eq 1 }
        }
        printLog(res)
    }

    @Test
    fun checkMatchContains() {
        EnumMMRRank.entries.forEach {
            printLog("${it.nameRank} - ${it.ordinal} - ${((it.ordinal / 10.0) * 2.0) + 1.0}")
        }
    }
}