package ru.descend.bot.postgre

import Entity
import column
import databases.Database
import ru.descend.bot.firebase.FireMatch
import save
import table

data class FireMatchTable(
    override var id: Int = 0,
    var matchId: String = "",
    var matchDate: Long = 0,
    var matchDuration: Long = 0,
    var matchMode: String = "",
    var matchGameVersion: String = "",
    var gameName: String = "",

    var guild: FireGuildTable? = null
) : Entity() {

    val participants: List<FireParticipantTable> by oneToMany(FireParticipantTable::match)

    companion object {
        fun getForId(id: Int) : FireMatchTable? {
            return fireMatchTable.first { FireMatchTable::id eq id }
        }
    }
}

val fireMatchTable = table<FireMatchTable, Database> {
    column(FireMatchTable::matchId).unique()
}