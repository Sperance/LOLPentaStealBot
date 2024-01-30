package ru.descend.bot.postgre.tables

import Entity
import column
import databases.Database
import table

data class TableMatch(
    override var id: Int = 0,
    var matchId: String = "",
    var matchDate: Long = 0,
    var matchDateEnd: Long = 0,
    var matchDuration: Int = 0,
    var matchMode: String = "",
    var matchGameVersion: String = "",
    var gameName: String = "",
    var bots: Boolean = false,
    var surrender: Boolean = false,

    var guild: TableGuild? = null
) : Entity() {

    val participants: List<TableParticipant> by oneToMany(TableParticipant::match)
}

val tableMatch = table<TableMatch, Database> {
    column(TableMatch::matchId).unique()
    column(TableMatch::guild).check { it neq null }
}