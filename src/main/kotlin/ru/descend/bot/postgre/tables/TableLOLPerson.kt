package ru.descend.bot.postgre.tables

import Entity
import column
import databases.Database
import ru.descend.bot.catchToken
import ru.descend.bot.lolapi.LeagueApi
import table

data class TableLOLPerson(
    override var id: Int = 0,

    var LOL_puuid: String = "",
    var LOL_summonerId: String = "",
    var LOL_accountId: String = "",
    var LOL_summonerName: String = "",
    var LOL_riotIdName: String? = "",
    var LOL_riotIdTagline: String? = "",
    var LOL_region: String? = ""
): Entity() {
    val KORDpersons: List<TableKORDPerson> by manyToMany(TableKORD_LOL::LOLperson, TableKORD_LOL::KORDperson)

    constructor(region: String, summonerName: String) : this() {
        val leagueApi = LeagueApi(catchToken()[1], region)
        leagueApi.leagueService.getBySummonerName(summonerName).execute().body()?.let {
            this.LOL_puuid = it.puuid
            this.LOL_summonerId = it.id
            this.LOL_accountId = it.accountId
            this.LOL_summonerName = it.name
            this.LOL_region = region
        }
    }

    fun isBot() : Boolean {
        if (LOL_puuid == "BOT" || LOL_puuid.length < 5){
            return true
        }
        if (LOL_summonerId == "BOT" || LOL_summonerId.length < 5){
            return true
        }
        return false
    }
}

val tableLOLPerson = table<TableLOLPerson, Database> {
    column(TableLOLPerson::LOL_puuid).unique()
}