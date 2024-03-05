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
    var bots: Boolean = false,
    var surrender: Boolean = false,

    var guild: TableGuild? = null
) : Entity() {

    val participants: List<TableParticipant> by oneToMany(TableParticipant::match)
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TableMatch

        if (id != other.id) return false
        if (matchId != other.matchId) return false
        if (matchDate != other.matchDate) return false
        if (matchDateEnd != other.matchDateEnd) return false
        if (matchDuration != other.matchDuration) return false
        if (matchMode != other.matchMode) return false
        if (matchGameVersion != other.matchGameVersion) return false
        if (bots != other.bots) return false
        if (surrender != other.surrender) return false
        if (guild != other.guild) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + matchId.hashCode()
        result = 31 * result + matchDate.hashCode()
        result = 31 * result + matchDateEnd.hashCode()
        result = 31 * result + matchDuration
        result = 31 * result + matchMode.hashCode()
        result = 31 * result + matchGameVersion.hashCode()
        result = 31 * result + bots.hashCode()
        result = 31 * result + surrender.hashCode()
        result = 31 * result + (guild?.hashCode() ?: 0)
        return result
    }
}

val tableMatch = table<TableMatch, Database> {
    column(TableMatch::matchId).unique()
    column(TableMatch::guild).check { it neq null }
}