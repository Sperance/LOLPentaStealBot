package ru.descend.bot.postgre.r2dbc.model

import dev.kord.common.entity.Snowflake
import dev.kord.core.cache.data.UserData
import dev.kord.core.entity.Guild
import dev.kord.core.entity.User
import org.komapper.annotation.KomapperAutoIncrement
import org.komapper.annotation.KomapperCreatedAt
import org.komapper.annotation.KomapperEntity
import org.komapper.annotation.KomapperId
import org.komapper.annotation.KomapperTable
import org.komapper.annotation.KomapperUpdatedAt
import org.komapper.core.dsl.Meta
import ru.descend.bot.postgre.R2DBC
import ru.descend.bot.datas.delete
import ru.descend.bot.datas.getData
import ru.descend.bot.postgre.r2dbc.model.KORDLOLs.Companion.tbl_kordlols
import ru.descend.bot.printLog
import java.time.LocalDateTime

@KomapperEntity
@KomapperTable("tbl_kords")
data class KORDs(
    @KomapperId
    @KomapperAutoIncrement
    val id: Int = 0,

    var KORD_id: String = "",
    var KORD_name: String = "",
    var KORD_discriminator: String = "",
    var date_birthday: String = "",
    var donations: Double = 0.0
) {

    companion object {
        val tbl_kords = Meta.korDs
    }

    constructor(user: User) : this() {
        this.KORD_id = user.id.value.toString()
        this.KORD_name = user.username
        this.KORD_discriminator = user.discriminator
    }

    fun asUser(guild: Guild) : User {
        return User(UserData(Snowflake(KORD_id), KORD_name), guild.kord)
    }

    suspend fun deleteWithKORDLOL(guilds: Guilds) {
        printLog("[KORDs::deleteWithKORDLOL] $this")
        KORDLOLs().getData({ tbl_kordlols.KORD_id eq id }).forEach {
            it.delete()
        }
        this.delete()
    }

    override fun toString(): String {
        return "KORDs(id=$id, KORD_id='$KORD_id', KORD_name='$KORD_name')"
    }
}