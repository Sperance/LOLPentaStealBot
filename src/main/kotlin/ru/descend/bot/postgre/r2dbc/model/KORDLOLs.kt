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
import ru.descend.bot.postgre.r2dbc.R2DBC
import ru.descend.bot.postgre.r2dbc.delete
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
    var mmrAram: Double = 0.0,
    var mmrAramSaved: Double = 0.0,
    var realtime_match_message: String = "",

    @KomapperCreatedAt
    var createdAt: LocalDateTime = LocalDateTime.MIN,
    @KomapperUpdatedAt
    var updatedAt: LocalDateTime = LocalDateTime.MIN
) {

    companion object {
        val tbl_kordlols = Meta.kordloLs
    }

    suspend fun deleteWithKORD(guilds: Guilds) {
        printLog("[KORDLOLs::deleteWithKORD] $this")
        R2DBC.getKORDs { tbl_kords.id eq KORD_id ; tbl_kords.guild_id eq guilds.id }.forEach {
            it.delete()
        }
        this.delete()
    }

    /*********************************/

    suspend fun KORDidObj() = R2DBC.getKORDs { tbl_kords.id eq KORD_id }.firstOrNull()
    suspend fun LOLidObj() = R2DBC.getLOLs { tbl_lols.id eq LOL_id }.firstOrNull()

    suspend fun asUser(guild: Guild, data: SQLData_R2DBC) : User {
        if (KORD_id == -1) throw ArgumentAccessException("KORDperson is NULL. KORDLOL_id: $id")
        return User(UserData(Snowflake(data.getKORD(KORD_id)!!.KORD_id.toLong()), data.getKORD(KORD_id)!!.KORD_name), guild.kord)
    }

    override fun toString(): String {
        return "KORDLOLs(id=$id, showCode=$showCode, mmrAram=$mmrAram, mmrAramSaved=$mmrAramSaved, KORD_id=$KORD_id, LOL_id=$LOL_id, guild_id=$guild_id)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KORDLOLs

        if (id != other.id) return false
        if (KORD_id != other.KORD_id) return false
        if (LOL_id != other.LOL_id) return false
        if (guild_id != other.guild_id) return false
        if (showCode != other.showCode) return false
        if (mmrAram != other.mmrAram) return false
        if (mmrAramSaved != other.mmrAramSaved) return false
        if (createdAt != other.createdAt) return false
        if (updatedAt != other.updatedAt) return false
        if (realtime_match_message != other.realtime_match_message) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + KORD_id
        result = 31 * result + LOL_id
        result = 31 * result + guild_id
        result = 31 * result + showCode
        result = 31 * result + mmrAram.hashCode()
        result = 31 * result + mmrAramSaved.hashCode()
        result = 31 * result + createdAt.hashCode()
        result = 31 * result + updatedAt.hashCode()
        result = 31 * result + realtime_match_message.hashCode()
        return result
    }
}