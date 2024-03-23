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
import org.komapper.core.dsl.QueryDsl
import ru.descend.bot.postgre.SQLData_R2DBC
import ru.descend.bot.postgre.r2dbc.R2DBC
import ru.descend.bot.postgre.r2dbc.interfaces.InterfaceR2DBC
import ru.descend.bot.printLog
import ru.descend.bot.savedObj.calculateUpdate
import java.time.LocalDateTime

val tbl_KORDLOLs = Meta.kordloLs

@KomapperEntity
@KomapperTable("tbl_KORDLOLs")
data class KORDLOLs(
    @KomapperId
    @KomapperAutoIncrement
    val id: Int = 0,

    var KORD_id: Int = -1,
    var LOL_id: Int = -1,
    var guild_id: Int = -1,

    var oldID: Int = 0,
    var showCode: Int = 0,
    var mmrAram: Double = 0.0,
    var mmrAramSaved: Double = 0.0,

    @KomapperCreatedAt
    var createdAt: LocalDateTime = LocalDateTime.MIN,
    @KomapperUpdatedAt
    var updatedAt: LocalDateTime = LocalDateTime.MIN
) : InterfaceR2DBC<KORDLOLs> {

    override suspend fun save() : KORDLOLs {
        val result = R2DBC.runTransaction {
            this.showCode = R2DBC.getKORDLOLs { tbl_KORDLOLs.guild_id eq this@KORDLOLs.guild_id }.size + 1
            R2DBC.runQuery(QueryDsl.insert(tbl_KORDLOLs).single(this@KORDLOLs))
        }
        printLog("[KORDLOLs::save] $result")
        return result
    }

    override suspend fun update() : KORDLOLs {
        val before = R2DBC.getKORDLOLs { tbl_KORDLOLs.id eq id }.firstOrNull()
        val after = R2DBC.runQuery(QueryDsl.update(tbl_KORDLOLs).single(this@KORDLOLs))
        printLog("[KORDLOLs::update] $this { ${calculateUpdate(before, after)} }")
        return after
    }

    override suspend fun delete() {
        printLog("[KORDLOLs::delete] $this")
        R2DBC.runQuery(QueryDsl.delete(tbl_KORDLOLs).single(this@KORDLOLs))
    }

    suspend fun deleteWithKORD(guilds: Guilds) {
        printLog("[KORDLOLs::deleteWithKORD] $this")
        R2DBC.getKORDs { tbl_KORDs.id eq KORD_id ; tbl_KORDs.guild_id eq guilds.id }.forEach {
            it.delete()
        }
        delete()
    }

    /*********************************/

    suspend fun getNickName(data: SQLData_R2DBC) : String {
        if (LOL_id == -1) return ""
        return if (data.getLOL(LOL_id)?.LOL_riotIdName.isNullOrEmpty()) data.getLOL(LOL_id)?.LOL_summonerName?:""
        else data.getLOL(LOL_id)?.LOL_riotIdName?:""
    }

    suspend fun getNickNameWithTag(data: SQLData_R2DBC) : String {
        return getNickName(data) + "#" + data.getLOL(LOL_id)?.LOL_riotIdTagline
    }

    suspend fun asUser(guild: Guild, data: SQLData_R2DBC) : User {
        if (KORD_id == -1) throw ArgumentAccessException("KORDperson is NULL. KORDLOL_id: $id")
        return User(UserData(Snowflake(data.getKORD(KORD_id)!!.KORD_id.toLong()), data.getKORD(KORD_id)!!.KORD_name), guild.kord)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KORDLOLs

        if (id != other.id) return false
        if (mmrAram != other.mmrAram) return false
        if (mmrAramSaved != other.mmrAramSaved) return false
        if (KORD_id != other.KORD_id) return false
        if (LOL_id != other.LOL_id) return false
        if (guild_id != other.guild_id) return false
        if (createdAt != other.createdAt) return false
        if (updatedAt != other.updatedAt) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + mmrAram.hashCode()
        result = 31 * result + mmrAramSaved.hashCode()
        result = 31 * result + KORD_id
        result = 31 * result + LOL_id
        result = 31 * result + guild_id
        result = 31 * result + createdAt.hashCode()
        result = 31 * result + updatedAt.hashCode()
        return result
    }

    override fun toString(): String {
        return "KORDLOLs(id=$id, showCode=$showCode, mmrAram=$mmrAram, mmrAramSaved=$mmrAramSaved, KORD_id=$KORD_id, LOL_id=$LOL_id, guild_id=$guild_id)"
    }
}