package ru.descend.bot.postgre

import Entity
import column
import databases.Database
import dev.kord.core.entity.Guild
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
        fun getForGuild(guild: Guild) : List<FireMatchTable> {
            return fireMatchTable.getAll { FireMatchTable::guild eq fireGuildTable.first { FireGuildTable::idGuild eq guild.id.value.toString() } }
        }

        fun getForMatchId(matchId: String) : FireMatchTable? {
            return fireMatchTable.first { FireMatchTable::matchId eq matchId }
        }
    }
}

val fireMatchTable = table<FireMatchTable, Database> {
    column(FireMatchTable::matchId).unique()
}