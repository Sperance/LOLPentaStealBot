package ru.descend.bot.postgre.r2dbc.model

import dev.kord.core.entity.Guild
import org.komapper.annotation.KomapperAutoIncrement
import org.komapper.annotation.KomapperCreatedAt
import org.komapper.annotation.KomapperEntity
import org.komapper.annotation.KomapperId
import org.komapper.annotation.KomapperTable
import org.komapper.annotation.KomapperUpdatedAt
import org.komapper.core.dsl.Meta
import ru.descend.bot.datas.create
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
    var messageIdTopUpdated: Long = 0
) {

    companion object {
        val tbl_guilds = Meta.guilds
    }

    suspend fun add(value: Guild) : Guilds {
        val curGuild = Guilds()
        curGuild.idGuild = value.id.value.toString()
        curGuild.name = value.name
        curGuild.description = value.description ?: ""
        return curGuild.create(Guilds::idGuild).result
    }

    override fun toString(): String {
        return "Guilds(id=$id, idGuild='$idGuild', name='$name', botChannedId='$botChannelId')"
    }
}