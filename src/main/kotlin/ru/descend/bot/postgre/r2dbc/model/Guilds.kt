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
import ru.descend.bot.asyncLaunch
import ru.descend.bot.lolapi.LeagueMainObject
import ru.descend.bot.lolapi.leaguedata.match_dto.MatchDTO
import ru.descend.bot.lolapi.leaguedata.match_dto.Participant
import ru.descend.bot.lowDescriptor
import ru.descend.bot.mail.GMailSender
import ru.descend.bot.postgre.SQLData_R2DBC
import ru.descend.bot.postgre.r2dbc.R2DBC
import ru.descend.bot.postgre.r2dbc.interfaces.InterfaceR2DBC
import ru.descend.bot.printLog
import ru.descend.bot.savedObj.CalculateMMR_2
import ru.descend.bot.savedObj.calculateUpdate
import ru.descend.bot.savedObj.isCurrentDay
import ru.descend.bot.sendMessage
import ru.descend.bot.toDate
import ru.descend.bot.toFormatDateTime
import java.time.LocalDateTime

val tbl_guilds = Meta.guilds

@KomapperEntity
@KomapperTable("tbl_guilds")
data class Guilds(
    @KomapperId
    @KomapperAutoIncrement
    var id: Int = 0,

    var idGuild: String = "",
    var name: String = "",
    var description: String = "",
    var ownerId: String = "",

    var botChannelId: String = "",
    var messageId: String = "",
    var messageIdStatus: String = "",
    var messageIdMain: String = "",
    var messageIdDebug: String = "",
    var messageIdPentaData: String = "",
    var messageIdGlobalStatisticData: String = "",
    var messageIdMasteryData: String = "",
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
        curGuild.ownerId = value.ownerId.value.toString()
        return curGuild.save()
    }

    override suspend fun save() : Guilds {
        val result = R2DBC.db.withTransaction {
            R2DBC.db.runQuery { QueryDsl.insert(tbl_guilds).single(this@Guilds) }
        }
        this.id = result.id
        this.updatedAt = result.updatedAt
        this.createdAt = result.createdAt
        printLog("[Guilds::save] $this")
        return this
    }

    override suspend fun update() : Guilds {
        val before = R2DBC.getGuilds { tbl_guilds.id eq this@Guilds.id }.firstOrNull()
        printLog("[Guilds::update] $this { ${calculateUpdate(before, this)} }")
        return R2DBC.db.withTransaction {
            R2DBC.db.runQuery { QueryDsl.update(tbl_guilds).single(this@Guilds) }
        }
    }

    override suspend fun delete() {
        printLog("[Guilds::delete] $this")
        R2DBC.db.withTransaction {
            R2DBC.db.runQuery { QueryDsl.delete(tbl_guilds).single(this@Guilds) }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Guilds

        if (id != other.id) return false
        if (idGuild != other.idGuild) return false
        if (name != other.name) return false
        if (description != other.description) return false
        if (ownerId != other.ownerId) return false
        if (botChannelId != other.botChannelId) return false
        if (messageId != other.messageId) return false
        if (messageIdStatus != other.messageIdStatus) return false
        if (messageIdMain != other.messageIdMain) return false
        if (messageIdDebug != other.messageIdDebug) return false
        if (messageIdPentaData != other.messageIdPentaData) return false
        if (messageIdGlobalStatisticData != other.messageIdGlobalStatisticData) return false
        if (messageIdMasteryData != other.messageIdMasteryData) return false
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
        result = 31 * result + ownerId.hashCode()
        result = 31 * result + botChannelId.hashCode()
        result = 31 * result + messageId.hashCode()
        result = 31 * result + messageIdStatus.hashCode()
        result = 31 * result + messageIdMain.hashCode()
        result = 31 * result + messageIdDebug.hashCode()
        result = 31 * result + messageIdPentaData.hashCode()
        result = 31 * result + messageIdGlobalStatisticData.hashCode()
        result = 31 * result + messageIdMasteryData.hashCode()
        result = 31 * result + messageIdArammmr.hashCode()
        result = 31 * result + createdAt.hashCode()
        result = 31 * result + updatedAt.hashCode()
        return result
    }

    override fun toString(): String {
        return "Guilds(id=$id, idGuild='$idGuild', name='$name')"
    }
}