package ru.descend.bot.postgre.tables

import Entity
import column
import databases.Database
import dev.kord.core.entity.Guild
import dev.kord.core.entity.User
import ru.descend.bot.postgre.getGuild
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
        this.guild = getGuild(guild)
        this.KORD_id = user.id.value.toString()
        this.KORD_name = user.username
        this.KORD_discriminator = user.discriminator
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TableKORDPerson

        if (id != other.id) return false
        if (KORD_id != other.KORD_id) return false
        if (KORD_name != other.KORD_name) return false
        if (KORD_discriminator != other.KORD_discriminator) return false
        if (guild != other.guild) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + KORD_id.hashCode()
        result = 31 * result + KORD_name.hashCode()
        result = 31 * result + KORD_discriminator.hashCode()
        result = 31 * result + (guild?.hashCode() ?: 0)
        return result
    }
}

val tableKORDPerson = table<TableKORDPerson, Database> {
    column(TableKORDPerson::KORD_id).unique()
    column(TableKORDPerson::guild).check { it neq null }
}