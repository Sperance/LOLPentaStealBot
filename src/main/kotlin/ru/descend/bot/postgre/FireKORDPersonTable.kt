package ru.descend.bot.postgre

import Entity
import column
import databases.Database
import dev.kord.core.entity.Guild
import ru.descend.bot.firebase.FirePerson
import table

data class FireKORDPersonTable(
    override var id: Int = 0,

    var KORD_id: String = "",
    var KORD_name: String = "",
    var KORD_discriminator: String = "",

    var guild: FireGuildTable? = null
): Entity() {
    val LOLpersons: List<FireLOLPersonTable> by manyToMany(FireKORD_LOLPersonTable::KORDperson, FireKORD_LOLPersonTable::LOLperson)

    constructor(guild: Guild, user: FirePerson) : this() {
        this.guild = LoadPostgreHistory.getGuild(guild)
        this.KORD_id = user.KORD_id
        this.KORD_name = user.KORD_name
        this.KORD_discriminator = user.KORD_discriminator
    }
}

val fireKORDPersonTable = table<FireKORDPersonTable, Database> {
    column(FireKORDPersonTable::KORD_id).unique()
}