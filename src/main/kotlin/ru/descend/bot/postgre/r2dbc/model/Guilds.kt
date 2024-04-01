package ru.descend.bot.postgre.r2dbc.model

import dev.kord.core.entity.Guild
import org.komapper.annotation.KomapperAutoIncrement
import org.komapper.annotation.KomapperCreatedAt
import org.komapper.annotation.KomapperEntity
import org.komapper.annotation.KomapperId
import org.komapper.annotation.KomapperTable
import org.komapper.annotation.KomapperUpdatedAt
import org.komapper.core.dsl.Meta
import org.komapper.core.dsl.QueryDsl
import ru.descend.bot.postgre.r2dbc.R2DBC
import ru.descend.bot.postgre.r2dbc.interfaces.InterfaceR2DBC
import ru.descend.bot.printLog
import ru.descend.bot.savedObj.calculateUpdate
import java.time.LocalDateTime

val tbl_guilds = Meta.guilds

@KomapperEntity
@KomapperTable("tbl_guilds")
data class Guilds(
    @KomapperId
    @KomapperAutoIncrement
    val id: Int = 0,

    var idGuild: String = "",
    var name: String = "",
    var description: String = "",

    var botChannelId: String = "",
    var messageIdStatus: String = "",
    var messageIdMain: String = "",
    var messageIdDebug: String = "",
    var messageIdGlobalStatisticData: String = "",
    var messageIdArammmr: String = "",

    @KomapperCreatedAt
    var createdAt: LocalDateTime = LocalDateTime.MIN,
    @KomapperUpdatedAt
    var updatedAt: LocalDateTime = LocalDateTime.MIN
) : InterfaceR2DBC<Guilds> {

    suspend fun add(value: Guild) : Guilds {
        val curGuild = Guilds()
        curGuild.idGuild = value.id.value.toString()
        curGuild.name = value.name
        curGuild.description = value.description ?: ""
        return curGuild.save()
    }

    override suspend fun save() : Guilds {
        val result = R2DBC.runQuery(QueryDsl.insert(tbl_guilds).single(this@Guilds))
        printLog("[Guilds::save] $result")
        return result
    }

    override suspend fun update() : Guilds {
        val before = R2DBC.getGuilds { tbl_guilds.id eq id }.firstOrNull()
        if (before == this) return this
        val after = R2DBC.runQuery(QueryDsl.update(tbl_guilds).single(this@Guilds))
        printLog("[Guilds::update] $this { ${calculateUpdate(before, after)} }")
        return after
    }

    override suspend fun delete() {
        printLog("[Guilds::delete] $this")
        R2DBC.runQuery(QueryDsl.delete(tbl_guilds).single(this@Guilds))
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Guilds

        if (id != other.id) return false
        if (idGuild != other.idGuild) return false
        if (name != other.name) return false
        if (description != other.description) return false
        if (botChannelId != other.botChannelId) return false
        if (messageIdStatus != other.messageIdStatus) return false
        if (messageIdMain != other.messageIdMain) return false
        if (messageIdDebug != other.messageIdDebug) return false
        if (messageIdGlobalStatisticData != other.messageIdGlobalStatisticData) return false
        if (messageIdArammmr != other.messageIdArammmr) return false
        if (createdAt != other.createdAt) return false
        if (updatedAt != other.updatedAt) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + idGuild.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + botChannelId.hashCode()
        result = 31 * result + messageIdStatus.hashCode()
        result = 31 * result + messageIdMain.hashCode()
        result = 31 * result + messageIdDebug.hashCode()
        result = 31 * result + messageIdGlobalStatisticData.hashCode()
        result = 31 * result + messageIdArammmr.hashCode()
        result = 31 * result + createdAt.hashCode()
        result = 31 * result + updatedAt.hashCode()
        return result
    }

    override fun toString(): String {
        return "Guilds(id=$id, idGuild='$idGuild', name='$name', botChannedId='$botChannelId')"
    }
}