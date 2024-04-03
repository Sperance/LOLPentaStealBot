package ru.descend.bot.postgre.r2dbc

import org.komapper.core.dsl.QueryDsl
import org.komapper.core.dsl.metamodel.EntityMetamodel
import org.komapper.core.dsl.metamodel.PropertyMetamodel
import org.komapper.core.dsl.metamodel.getAutoIncrementProperty
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
        if ((fieldbefore != fieldafter && it.name != "updatedAt" && it.name != "createdAt") || it.name == "champion") {
            result += "${it.name} '${fieldbefore}' -> '${fieldafter}'; "
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
    if (already == null) throw IllegalArgumentException("In table for name tbl_${this::class.java.simpleName.lowercase()}) dont find object with id ${this@update.getField("id")}")

    val result = R2DBC.runQuery { QueryDsl.update(metaTable).single(this@update) }
    printLog("[${this::class.java.simpleName}::${Thread.currentThread().stackTrace[1].methodName}] $this {${calculateUpdate(already, result)}}")
    return result as TYPE
}

@Suppress("UNCHECKED_CAST")
suspend fun <TYPE: Any, META : EntityMetamodel<Any, Any, META>> TYPE.create(kProperty1: KMutableProperty1<TYPE, *>): TYPE {
    val metaTable = getInstanceClassForTbl(this) as META

    val metaProperty = metaTable.properties().find { it.name == kProperty1.name } as PropertyMetamodel<Any, Any, META>
    val already = R2DBC.runQuery {
        QueryDsl.from(metaTable)
            .where { metaProperty eq kProperty1.get(this@create) }
            .limit(1)
    }.firstOrNull()

    if (already != null) {
        printLog("[${this::class.java.simpleName}::${Thread.currentThread().stackTrace[1].methodName}] $this - already having in SQL")
        return already as TYPE
    }

    val result = R2DBC.runQuery { QueryDsl.insert(metaTable).single(this@create) }
    printLog("[${this::class.java.simpleName}::${Thread.currentThread().stackTrace[1].methodName}] $result")
    return result as TYPE
}

@Suppress("UNCHECKED_CAST")
suspend fun <META : EntityMetamodel<Any, Any, META>> Any.delete() {
    val metaTable = getInstanceClassForTbl(this) as META

    printLog("[${this::class.java.simpleName}::${Thread.currentThread().stackTrace[1].methodName}] $this")

    R2DBC.runQuery {
        val prop_id = metaTable.getAutoIncrementProperty() as PropertyMetamodel<Any, Int, Int>
        QueryDsl.delete(metaTable).where { prop_id eq this.getField("id") as Int }
    }
}