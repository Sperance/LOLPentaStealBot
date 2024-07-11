package ru.descend.bot.datas

import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.komapper.core.dsl.QueryDsl
import org.komapper.core.dsl.expression.SortExpression
import org.komapper.core.dsl.expression.WhereDeclaration
import org.komapper.core.dsl.metamodel.EntityMetamodel
import org.komapper.core.dsl.metamodel.PropertyMetamodel
import org.komapper.core.dsl.metamodel.getAutoIncrementProperty
import org.komapper.core.dsl.operator.count
import org.komapper.core.dsl.query.firstOrNull
import ru.descend.bot.postgre.R2DBC
import ru.descend.bot.postgre.R2DBC.db
import ru.descend.bot.postgre.r2dbc.model.Guilds
import ru.descend.bot.postgre.r2dbc.model.Guilds.Companion.tbl_guilds
import ru.descend.bot.printLog
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.createInstance

fun Any.getField(name: String) = this::class.java.declaredFields.find { it.isAccessible = true ; it.name == name }?.get(this)

@Suppress("UNNECESSARY_NOT_NULL_ASSERTION")
fun <T> calculateUpdate(before: T?, after: T?) : String {
    if (before == null) return ""
    if (after == null) return ""
    var result = ""
    before!!::class.java.declaredFields.forEach {
        it.isAccessible = true
        val fieldbefore = it.get(before)
        val fieldafter = it.get(after)
        if (fieldbefore != fieldafter && it.name != "updatedAt") {
            result += "${it.name}:'${fieldbefore}'->'${fieldafter}' "
        }
    }
    return result
}

@Suppress("UNCHECKED_CAST")
fun <META : EntityMetamodel<Any, Any, META>> getInstanceClassForTbl(obj: Any) : META {
    val nameClass = "ru.descend.bot.postgre.r2dbc.model.${obj::class.java.simpleName}"
    val instance = Class.forName(nameClass).kotlin.createInstance()
    val metaTable = instance.getField("tbl_${obj::class.java.simpleName.lowercase()}")
        ?: throw IllegalArgumentException("not finded field with name tbl_${obj::class.java.simpleName.lowercase()}) in class $nameClass")

    return metaTable as META
}

@Suppress("UNCHECKED_CAST")
suspend fun <TYPE: Any, META : EntityMetamodel<Any, Any, META>> TYPE.update() : TYPE {
    val metaTable = getInstanceClassForTbl(this) as META

    val prop_id = metaTable.getAutoIncrementProperty() as PropertyMetamodel<Any, Int, Int>
    val already = R2DBC.runQuery {
        QueryDsl.from(metaTable)
            .where { prop_id eq this@update.getField("id") as Int }
            .limit(1)
    }.firstOrNull()

    if (already == this) return already as TYPE
    if (already == null) throw IllegalArgumentException("In table for name tbl_${this::class.java.simpleName.lowercase()}) in Meta ${metaTable.javaClass.simpleName} don`t find object with id ${this@update.getField("id")}. Object: $this")

    val result = R2DBC.runQuery { QueryDsl.update(metaTable).single(this@update) }
    printLog("[${this::class.java.simpleName}::${Thread.currentThread().stackTrace[1].methodName}] $this {${calculateUpdate(already, result)}}")
    return result as TYPE
}

data class CoreResult <T> (
    val message: String = "",
    val bit: Boolean,
    val result: T
)

@Suppress("UNCHECKED_CAST")
suspend fun <TYPE: Any, META : EntityMetamodel<Any, Any, META>> TYPE.create(kProperty1: KMutableProperty1<TYPE, *>?, appendText: String = ""): CoreResult<TYPE> {
    val metaTable = getInstanceClassForTbl(this) as META

    if (kProperty1 != null) {
        val metaProperty = metaTable.properties().find { it.name == kProperty1.name } as PropertyMetamodel<Any, Any, META>
        val already = R2DBC.runQuery {
            QueryDsl.from(metaTable)
                .where { metaProperty eq kProperty1.get(this@create) }
                .limit(1)
        }.firstOrNull()

        if (already != null) {
            return CoreResult(bit = false, result = already as TYPE)
        }
    }

    val result = R2DBC.runQuery { QueryDsl.insert(metaTable).single(this@create) }
    printLog("[${this::class.java.simpleName}::${Thread.currentThread().stackTrace[1].methodName}]$appendText $result")
    return CoreResult(bit = true, result = result as TYPE)
}

@Suppress("UNCHECKED_CAST")
suspend fun <META : EntityMetamodel<Any, Any, META>> Any.delete() {
    val metaTable = getInstanceClassForTbl(this) as META

    printLog("[${this::class.java.simpleName}::${Thread.currentThread().stackTrace[1].methodName}] $this")

    R2DBC.runQuery {
        val prop_id = metaTable.getAutoIncrementProperty() as PropertyMetamodel<Any, Int, Int>
        QueryDsl.delete(metaTable).where { prop_id eq this@delete.getField("id") as Int }
    }
}

@Suppress("UNCHECKED_CAST")
suspend fun <TYPE: Any, META : EntityMetamodel<Any, Any, META>> TYPE.getData(declaration: WhereDeclaration? = null, sortExpression: SortExpression? = null) : List<TYPE> {
    val metaTable = getInstanceClassForTbl(this) as META
    val whereExpr = declaration ?: {metaTable.getAutoIncrementProperty() as PropertyMetamodel<Any, Int, Int> greaterEq 0}
    val sortExpr = sortExpression ?: metaTable.getAutoIncrementProperty()!!
    return R2DBC.runQuery { QueryDsl.from(metaTable).where(whereExpr).orderBy(sortExpr) } as List<TYPE>
}

@Suppress("UNCHECKED_CAST")
suspend fun <TYPE: Any, META : EntityMetamodel<Any, Any, META>> TYPE.getDataOne(declaration: WhereDeclaration? = null, sortExpression: SortExpression? = null) : TYPE? {
    val metaTable = getInstanceClassForTbl(this) as META
    val whereExpr: WhereDeclaration = declaration ?: {metaTable.getAutoIncrementProperty() as PropertyMetamodel<Any, Int, Int> greaterEq 0}
    val sortExpr = sortExpression ?: metaTable.getAutoIncrementProperty()!!
    return R2DBC.runQuery { QueryDsl.from(metaTable).where(whereExpr).orderBy(sortExpr).limit(1).firstOrNull() } as TYPE?
}

@Suppress("UNCHECKED_CAST")
suspend fun <TYPE: Any, META : EntityMetamodel<Any, Any, META>> TYPE.getSize(declaration: WhereDeclaration? = null) : Long {
    val metaTable = getInstanceClassForTbl(this) as META
    val whereExpr: WhereDeclaration = declaration ?: {metaTable.getAutoIncrementProperty() as PropertyMetamodel<Any, Int, Int> greaterEq 0}
    return R2DBC.runQuery { QueryDsl.from(metaTable).where(whereExpr).select(count()) }?:0L
}

@Suppress("UNCHECKED_CAST")
suspend inline fun <reified TYPE: Any, META : EntityMetamodel<Any, Any, META>> TYPE.addBatch(list: List<TYPE>, batchSize: Int = 100, printLog: Boolean = true) : List<TYPE> {
    val metaTable = getInstanceClassForTbl(this) as META
    return db.withTransaction {
        val miniList = ArrayList<TYPE>()
        val resultedList = ArrayList<TYPE>()
        list.forEach { value ->
            miniList.add(value)
            if (miniList.size == batchSize) {
                val res = db.runQuery { QueryDsl.insert(metaTable).multiple(miniList) } as List<TYPE>
                resultedList.addAll(res)
                if (printLog) res.forEach { printLog("\t[Batch_${TYPE::class.java.simpleName}::save] $it", false) }
                miniList.clear()
            }
        }
        if (miniList.isNotEmpty()) {
            val res = db.runQuery { QueryDsl.insert(metaTable).multiple(miniList) } as List<TYPE>
            resultedList.addAll(res)
            if (printLog) res.forEach { printLog("\t[Batch_${TYPE::class.java.simpleName}::save] $it", false) }
        }
        resultedList
    }
}

class CoreMethods {
    @Test
    fun test_Core(){
        runBlocking {
            val data = Guilds().getData(declaration = { tbl_guilds.id eq 2 }, sortExpression = tbl_guilds.id)
            data.forEach {
                println("data: $it")
            }
            println("SIZE: ${Guilds().getSize{tbl_guilds.id greaterEq 2}}")
        }
    }
}