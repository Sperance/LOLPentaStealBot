package ru.descend.bot.postgre

import Entity
import column
import databases.Database
import ru.descend.bot.firebase.FirePerson
import table

data class FireLOLPersonTable(
    override var id: Int = 0,

    var LOL_puuid: String = "",
    var LOL_summonerId: String = "",
    var LOL_summonerName: String = "",
    var LOL_riotIdName: String? = "",
    var LOL_riotIdTagline: String? = ""
): Entity() {
    val KORDpersons: List<FireKORDPersonTable> by manyToMany(FireKORD_LOLPersonTable::LOLperson, FireKORD_LOLPersonTable::KORDperson)

    constructor(user: FirePerson) : this() {
        this.LOL_puuid = user.LOL_puuid
        this.LOL_summonerId = user.LOL_accountId
        this.LOL_summonerName = user.LOL_name
    }
}

val fireLOLPersonTable = table<FireLOLPersonTable, Database> {
    column(FireLOLPersonTable::LOL_puuid).unique()
}