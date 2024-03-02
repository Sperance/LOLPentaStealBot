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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TableLOLPerson

        if (id != other.id) return false
        if (LOL_puuid != other.LOL_puuid) return false
        if (LOL_summonerId != other.LOL_summonerId) return false
        if (LOL_accountId != other.LOL_accountId) return false
        if (LOL_summonerName != other.LOL_summonerName) return false
        if (LOL_riotIdName != other.LOL_riotIdName) return false
        if (LOL_riotIdTagline != other.LOL_riotIdTagline) return false
        if (LOL_region != other.LOL_region) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + LOL_puuid.hashCode()
        result = 31 * result + LOL_summonerId.hashCode()
        result = 31 * result + LOL_accountId.hashCode()
        result = 31 * result + LOL_summonerName.hashCode()
        result = 31 * result + (LOL_riotIdName?.hashCode() ?: 0)
        result = 31 * result + (LOL_riotIdTagline?.hashCode() ?: 0)
        result = 31 * result + (LOL_region?.hashCode() ?: 0)
        return result
    }
}

val tableLOLPerson = table<TableLOLPerson, Database> {
    column(TableLOLPerson::LOL_puuid).unique()
}