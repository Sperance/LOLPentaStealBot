package ru.descend.bot.postgre.r2dbc.model

import org.komapper.annotation.KomapperAutoIncrement
import org.komapper.annotation.KomapperCreatedAt
import org.komapper.annotation.KomapperEntity
import org.komapper.annotation.KomapperId
import org.komapper.annotation.KomapperTable
import org.komapper.annotation.KomapperUpdatedAt
import org.komapper.core.dsl.Meta
import org.komapper.core.dsl.QueryDsl
import org.komapper.r2dbc.R2dbcDatabase
import ru.descend.bot.postgre.r2dbc.R2DBC
import ru.descend.bot.printLog
import java.time.LocalDateTime

val tbl_KORDs = Meta.korDs

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

    suspend fun update() : KORDs? {
        var result: KORDs? = null
        R2DBC.db.withTransaction {
            result = R2DBC.db.runQuery {
                QueryDsl.update(tbl_KORDs).single(this@KORDs)
            }
            printLog("[KORDs::update] updated KORD id ${result?.id}")
        }
        return result
    }

    companion object {
        suspend fun resetData(guild: Guilds) : List<KORDs> {
            return R2DBC.db.withTransaction {
                R2DBC.db.runQuery {
                    QueryDsl.from(tbl_KORDs).where { tbl_KORDs.guild_id eq guild.id }
                }
            }
        }
    }

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