package ru.descend.bot.postgre

import dev.kord.core.entity.Guild
import ru.descend.bot.printLog

object PostgreSQL {
    fun getGuild(guild: Guild) : TableGuild {
        val myGuild = tableGuild.first { TableGuild::idGuild eq guild.id.value.toString() }
        if (myGuild == null) {
            printLog(guild, "[PostgreSQL] Creating Guild with id ${guild.id.value}")
            TableGuild().initGuild(guild)
            return tableGuild.first { TableGuild::idGuild eq guild.id.value.toString() }!!
        }
        return myGuild
    }
}