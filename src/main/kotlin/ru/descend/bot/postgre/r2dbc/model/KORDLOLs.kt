package ru.descend.bot.postgre.r2dbc.model

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.cache.data.UserData
import dev.kord.core.entity.User
import org.junit.jupiter.params.aggregator.ArgumentAccessException
import org.komapper.annotation.KomapperAutoIncrement
import org.komapper.annotation.KomapperEntity
import org.komapper.annotation.KomapperId
import org.komapper.annotation.KomapperTable
import org.komapper.core.dsl.Meta
import ru.descend.bot.postgre.SQLData_R2DBC
import ru.descend.bot.printLog
import ru.descend.bot.sqlData

@KomapperEntity
@KomapperTable("tbl_kordlols")
data class KORDLOLs(
    @KomapperId
    @KomapperAutoIncrement
    val id: Int = 0,

    var KORD_id: Int = -1,
    var LOL_id: Int = -1
) {

    companion object {
        val tbl_kordlols = Meta.kordloLs
    }

    /*********************************/

    suspend fun asUser(data: SQLData_R2DBC) : User {
        if (KORD_id == -1) throw ArgumentAccessException("KORDperson is NULL. KORDLOL_id: $id")
        return User(UserData(Snowflake(data.getKORD(KORD_id)!!.KORD_id.toLong()), data.getKORD(KORD_id)!!.KORD_name), sqlData?.guild?.kord?: Kord(""))
    }

    override fun toString(): String {
        return "KORDLOLs(id=$id, KORD_id=$KORD_id, LOL_id=$LOL_id)"
    }
}