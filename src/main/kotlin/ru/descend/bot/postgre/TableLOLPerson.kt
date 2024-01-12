package ru.descend.bot.postgre

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
    var LOL_summonerName: String = "",
    var LOL_riotIdName: String? = "",
    var LOL_riotIdTagline: String? = "",
    var LOL_region: String? = ""
): Entity() {
    val KORDpersons: List<TableKORDPerson> by manyToMany(TableKORD_LOL::LOLperson, TableKORD_LOL::KORDperson)

    fun initLOL(region: String, summonerName: String) {
        val leagueApi = LeagueApi(catchToken()[1], region)
        leagueApi.leagueService.getBySummonerName(summonerName).execute().body()?.let {
            this.LOL_puuid = it.puuid
            this.LOL_summonerId = it.id
            this.LOL_summonerName = it.name
            this.LOL_region = region
        }
    }
}

val tableLOLPerson = table<TableLOLPerson, Database> {
    column(TableLOLPerson::LOL_puuid).unique()
}