package ru.descend.bot.postgre

import Entity
import column
import databases.Database
import dev.kord.core.entity.Guild
import dev.kord.core.entity.User
import table

data class TableKORDPerson(
    override var id: Int = 0,

    var KORD_id: String = "",
    var KORD_name: String = "",
    var KORD_discriminator: String = "",

    var guild: TableGuild? = null
): Entity() {
    val LOLpersons: List<TableLOLPerson> by manyToMany(TableKORD_LOL::KORDperson, TableKORD_LOL::LOLperson)

    constructor(guild: Guild, user: User) : this() {
        this.guild = PostgreSQL.getGuild(guild)
        this.KORD_id = user.id.value.toString()
        this.KORD_name = user.username
        this.KORD_discriminator = user.discriminator
    }
}

val tableKORDPerson = table<TableKORDPerson, Database> {
    column(TableKORDPerson::KORD_id).unique()
    column(TableKORDPerson::guild).check { it neq null }
}