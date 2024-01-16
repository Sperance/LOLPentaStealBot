package ru.descend.bot.postgre

import Entity
import column
import databases.Database
import dev.kord.core.entity.Guild
import ru.descend.bot.postgre.PostgreSQL.getGuild
import table

data class TableMatch(
    override var id: Int = 0,
    var matchId: String = "",
    var matchDate: Long = 0,
    var matchDuration: Int = 0,
    var matchMode: String = "",
    var matchGameVersion: String = "",
    var gameName: String = "",

    var guild: TableGuild? = null
) : Entity() {

    val messages: List<TableMessage> by oneToMany(TableMessage::match)
    val participants: List<TableParticipant> by oneToMany(TableParticipant::match)

    fun isHaveBots(arrayParts: ArrayList<TableParticipant>) : Boolean {
        arrayParts.filter { it.match!!.matchId == matchId }.forEach {
            if (it.LOLperson!!.isBot()) return true
        }
        return false
    }
}

val tableMatch = table<TableMatch, Database> {
    column(TableMatch::matchId).unique()
    column(TableMatch::guild).check { it neq null }
}