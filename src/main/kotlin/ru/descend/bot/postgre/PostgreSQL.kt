package ru.descend.bot.postgre

import dev.kord.core.entity.Guild
import kotlinx.coroutines.delay
import ru.descend.bot.postgre.tables.TableGuild
import ru.descend.bot.postgre.tables.tableGuild
import ru.descend.bot.printLog
import java.sql.ResultSet
import java.sql.Statement

fun getGuild(guild: Guild): TableGuild {
    val myGuild = tableGuild.first { TableGuild::idGuild eq guild.id.value.toString() }
    if (myGuild == null) {
        printLog(guild, "[PostgreSQL] Creating Guild with id ${guild.id.value}")
        TableGuild().initGuild(guild)
        return tableGuild.first { TableGuild::idGuild eq guild.id.value.toString() }!!
    }
    return myGuild
}

fun execProcedure(text: String) {
    printLog("[execProcedure] $text")
    val state = Postgre.newStatement
    state.queryTimeout = 10
    state.execute(text)
}

fun execQuery(query: String, body: (ResultSet?) -> Unit) {
    printLog("[execQuery] $query")
    val state = Postgre.newStatement
    state.queryTimeout = 10
    val value = state.executeQuery(query)
    body.invoke(value)
}