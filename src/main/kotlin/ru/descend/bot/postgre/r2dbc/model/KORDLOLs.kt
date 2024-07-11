package ru.descend.bot.postgre.r2dbc.model

import dev.kord.common.entity.Snowflake
import dev.kord.core.cache.data.UserData
import dev.kord.core.entity.Guild
import dev.kord.core.entity.User
import org.junit.jupiter.params.aggregator.ArgumentAccessException
import org.komapper.annotation.KomapperAutoIncrement
import org.komapper.annotation.KomapperCreatedAt
import org.komapper.annotation.KomapperEntity
import org.komapper.annotation.KomapperId
import org.komapper.annotation.KomapperTable
import org.komapper.annotation.KomapperUpdatedAt
import org.komapper.core.dsl.Meta
import ru.descend.bot.postgre.SQLData_R2DBC
import ru.descend.bot.postgre.R2DBC
import ru.descend.bot.datas.delete
import ru.descend.bot.postgre.r2dbc.model.KORDs.Companion.tbl_kords
import ru.descend.bot.postgre.r2dbc.model.LOLs.Companion.tbl_lols
import ru.descend.bot.printLog
import java.time.LocalDateTime

@KomapperEntity
@KomapperTable("tbl_kordlols")
data class KORDLOLs(
    @KomapperId
    @KomapperAutoIncrement
    val id: Int = 0,

    var KORD_id: Int = -1,
    var LOL_id: Int = -1,
    var guild_id: Int = -1,

    var showCode: Int = 0,
    var realtime_match_message: String = ""
) {

    companion object {
        val tbl_kordlols = Meta.kordloLs
    }

    /*********************************/

    suspend fun asUser(data: SQLData_R2DBC) : User {
        if (KORD_id == -1) throw ArgumentAccessException("KORDperson is NULL. KORDLOL_id: $id")
        return User(UserData(Snowflake(data.getKORD(KORD_id)!!.KORD_id.toLong()), data.getKORD(KORD_id)!!.KORD_name), data.guild.kord)
    }

    override fun toString(): String {
        return "KORDLOLs(id=$id, showCode=$showCode, KORD_id=$KORD_id, LOL_id=$LOL_id, guild_id=$guild_id)"
    }
}