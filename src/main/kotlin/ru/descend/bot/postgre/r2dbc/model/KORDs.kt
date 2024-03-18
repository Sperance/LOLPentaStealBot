package ru.descend.bot.postgre.r2dbc.model

import org.komapper.annotation.KomapperAutoIncrement
import org.komapper.annotation.KomapperCreatedAt
import org.komapper.annotation.KomapperEntity
import org.komapper.annotation.KomapperId
import org.komapper.annotation.KomapperTable
import org.komapper.annotation.KomapperUpdatedAt
import java.time.LocalDateTime

@KomapperEntity
@KomapperTable("tbl_KORDs")
data class KORDs(
    @KomapperId
    @KomapperAutoIncrement
    val id: Int = 0,

    var KORD_id: String = "",
    var KORD_name: String = "",
    var KORD_discriminator: String = "",

    var guild_id: Int = -1,

    @KomapperCreatedAt
    val createdAt: LocalDateTime = LocalDateTime.MIN,
    @KomapperUpdatedAt
    val updatedAt: LocalDateTime = LocalDateTime.MIN
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KORDs

        if (id != other.id) return false
        if (KORD_id != other.KORD_id) return false
        if (KORD_name != other.KORD_name) return false
        if (KORD_discriminator != other.KORD_discriminator) return false
        if (guild_id != other.guild_id) return false
        if (createdAt != other.createdAt) return false
        if (updatedAt != other.updatedAt) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + KORD_id.hashCode()
        result = 31 * result + KORD_name.hashCode()
        result = 31 * result + KORD_discriminator.hashCode()
        result = 31 * result + guild_id
        result = 31 * result + createdAt.hashCode()
        result = 31 * result + updatedAt.hashCode()
        return result
    }
}