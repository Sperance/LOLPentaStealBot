package ru.descend.bot.postgre.r2dbc.model

import dev.kord.core.entity.User
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

val tbl_KORDs = Meta.korDs

@KomapperEntity
@KomapperTable("tbl_KORDs")
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
) : InterfaceR2DBC<KORDs> {

    companion object {
        suspend fun isHave(kord: String) = R2DBC.runQuery { QueryDsl.from(tbl_KORDs).where { tbl_KORDs.KORD_id eq kord }.limit(1).select(tbl_KORDs.id) }.isNotEmpty()
    }

    constructor(guild: Guilds, user: User) : this() {
        this.guild_id = guild.id
        this.KORD_id = user.id.value.toString()
        this.KORD_name = user.username
        this.KORD_discriminator = user.discriminator
    }

    override suspend fun save() : KORDs {
        return if (isHave(KORD_id)) {
            printLog("[KORDs::save] $this. Уже существует в базе. Вызывать ошибку?")
            this
        } else {
            val result = R2DBC.runQuery(QueryDsl.insert(tbl_KORDs).single(this@KORDs))
            printLog("[KORDs::save] $result")
            result
        }
    }

    override suspend fun update() : KORDs {
        val before = R2DBC.getKORDs { tbl_KORDs.id eq id }.firstOrNull()
        if (before == this) return this
        val after = R2DBC.runQuery(QueryDsl.update(tbl_KORDs).single(this@KORDs))
        printLog("[KORDs::update] $this { ${calculateUpdate(before, after)} }")
        return after
    }

    override suspend fun delete() {
        printLog("[KORDs::delete] $this")
        R2DBC.runQuery(QueryDsl.delete(tbl_KORDs).single(this@KORDs))
    }

    suspend fun deleteWithKORDLOL(guilds: Guilds) {
        printLog("[KORDs::deleteWithKORDLOL] $this")
        R2DBC.getKORDLOLs { tbl_KORDLOLs.KORD_id eq id ; tbl_KORDLOLs.guild_id eq guilds.id }.forEach {
            it.delete()
        }
        delete()
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