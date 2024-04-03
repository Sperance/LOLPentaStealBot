package ru.descend.bot.postgre.r2dbc.model

import dev.kord.core.entity.User
import org.komapper.annotation.KomapperAutoIncrement
import org.komapper.annotation.KomapperCreatedAt
import org.komapper.annotation.KomapperEntity
import org.komapper.annotation.KomapperId
import org.komapper.annotation.KomapperTable
import org.komapper.annotation.KomapperUpdatedAt
import org.komapper.core.dsl.Meta
import ru.descend.bot.postgre.r2dbc.R2DBC
import ru.descend.bot.postgre.r2dbc.delete
import ru.descend.bot.postgre.r2dbc.model.KORDLOLs.Companion.tbl_kordlols
import ru.descend.bot.printLog
import java.time.LocalDateTime

@KomapperEntity
@KomapperTable("tbl_kords")
data class KORDs(
    @KomapperId
    @KomapperAutoIncrement
    val id: Int = 0,

    var guild_id: Int = -1,

    var KORD_id: String = "",
    var KORD_name: String = "",
    var KORD_discriminator: String = "",
    var date_birthday: String = "",

    @KomapperCreatedAt
    var createdAt: LocalDateTime = LocalDateTime.MIN,
    @KomapperUpdatedAt
    var updatedAt: LocalDateTime = LocalDateTime.MIN
) {

    companion object {
        val tbl_kords = Meta.korDs
    }

    constructor(guild: Guilds, user: User) : this() {
        this.guild_id = guild.id
        this.KORD_id = user.id.value.toString()
        this.KORD_name = user.username
        this.KORD_discriminator = user.discriminator
    }

    suspend fun deleteWithKORDLOL(guilds: Guilds) {
        printLog("[KORDs::deleteWithKORDLOL] $this")
        R2DBC.getKORDLOLs { tbl_kordlols.KORD_id eq id ; tbl_kordlols.guild_id eq guilds.id }.forEach {
            it.delete()
        }
        this.delete()
    }

    override fun toString(): String {
        return "KORDs(id=$id, KORD_id='$KORD_id', KORD_name='$KORD_name', guild_id=$guild_id)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KORDs

        if (id != other.id) return false
        if (guild_id != other.guild_id) return false
        if (KORD_id != other.KORD_id) return false
        if (KORD_name != other.KORD_name) return false
        if (KORD_discriminator != other.KORD_discriminator) return false
        if (createdAt != other.createdAt) return false
        if (updatedAt != other.updatedAt) return false
        if (date_birthday != other.date_birthday) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + guild_id
        result = 31 * result + KORD_id.hashCode()
        result = 31 * result + KORD_name.hashCode()
        result = 31 * result + KORD_discriminator.hashCode()
        result = 31 * result + createdAt.hashCode()
        result = 31 * result + updatedAt.hashCode()
        result = 31 * result + date_birthday.hashCode()
        return result
    }
}