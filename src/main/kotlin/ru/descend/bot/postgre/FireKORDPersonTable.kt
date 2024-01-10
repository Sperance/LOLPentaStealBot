package ru.descend.bot.postgre

import Entity
import column
import databases.Database
import ru.descend.bot.firebase.FirePerson
import table

data class FireKORDPersonTable(
    override var id: Int = 0,

    var KORD_id: String = "",
    var KORD_name: String = "",
    var KORD_discriminator: String = "",
): Entity() {
    val LOLpersons: List<FireLOLPersonTable> by manyToMany(FireKORD_LOLPersonTable::KORDperson, FireKORD_LOLPersonTable::LOLperson)

    constructor(user: FirePerson) : this() {
        this.KORD_id = user.KORD_id
        this.KORD_name = user.KORD_name
        this.KORD_discriminator = user.KORD_discriminator
    }

    companion object {
        fun getForId(id: Int) : FireKORDPersonTable? {
            return fireKORDPersonTable.first { FireKORDPersonTable::id eq id }
        }
    }
}

val fireKORDPersonTable = table<FireKORDPersonTable, Database> {
    column(FireKORDPersonTable::KORD_id).unique()
}