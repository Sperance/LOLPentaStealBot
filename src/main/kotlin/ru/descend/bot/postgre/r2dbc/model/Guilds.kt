package ru.descend.bot.postgre.r2dbc.model

import dev.kord.core.entity.Guild
import org.komapper.annotation.KomapperAutoIncrement
import org.komapper.annotation.KomapperCreatedAt
import org.komapper.annotation.KomapperEntity
import org.komapper.annotation.KomapperId
import org.komapper.annotation.KomapperTable
import org.komapper.annotation.KomapperUpdatedAt
import org.komapper.core.dsl.Meta
import ru.descend.bot.postgre.r2dbc.create
import java.time.LocalDateTime

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
    /**
     * Канал основных сообщений на сервере (видимых всем пользователям)
     */
    var messageIdStatus: String = "",
    /**
     * ID Имя Серия побед таблица
     */
    var messageIdMain: String = "",
    /**
     * Канал для служебных сообщений (скрыт для обычных пользователей)
     */
    var messageIdDebug: String = "",
    /**
     * Таблица по всем играм\винрейт\сериям убийств
     */
    var messageIdGlobalStatisticData: String = "",
    /**
     * Таблица ММР арама и последней игры
     */
    var messageIdArammmr: String = "",
    /**
     * Таблица мастерства ТОП 3 чемпионов
     */
    var messageIdMasteries: String = "",
    var messageIdMasteriesUpdated: Long = 0,
    /**
     * Таблица ТОПА сервера
     */
    var messageIdTop: String = "",
    var messageIdTopUpdated: Long = 0,

    @KomapperCreatedAt
    var createdAt: LocalDateTime = LocalDateTime.MIN,
    @KomapperUpdatedAt
    var updatedAt: LocalDateTime = LocalDateTime.MIN
) {

    companion object {
        val tbl_guilds = Meta.guilds
    }

    suspend fun add(value: Guild) : Guilds {
        val curGuild = Guilds()
        curGuild.idGuild = value.id.value.toString()
        curGuild.name = value.name
        curGuild.description = value.description ?: ""
        return curGuild.create(Guilds::idGuild)
    }

    override fun toString(): String {
        return "Guilds(id=$id, idGuild='$idGuild', name='$name', botChannedId='$botChannelId')"
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
        if (messageIdMasteries != other.messageIdMasteries) return false
        if (messageIdMasteriesUpdated != other.messageIdMasteriesUpdated) return false
        if (messageIdTop != other.messageIdTop) return false
        if (messageIdTopUpdated != other.messageIdTopUpdated) return false
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
        result = 31 * result + messageIdMasteries.hashCode()
        result = 31 * result + messageIdMasteriesUpdated.hashCode()
        result = 31 * result + messageIdTop.hashCode()
        result = 31 * result + messageIdTopUpdated.hashCode()
        result = 31 * result + createdAt.hashCode()
        result = 31 * result + updatedAt.hashCode()
        return result
    }
}