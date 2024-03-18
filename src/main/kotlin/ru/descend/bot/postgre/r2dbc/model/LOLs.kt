package ru.descend.bot.postgre.r2dbc.model

import org.komapper.annotation.KomapperAutoIncrement
import org.komapper.annotation.KomapperCreatedAt
import org.komapper.annotation.KomapperEntity
import org.komapper.annotation.KomapperId
import org.komapper.annotation.KomapperTable
import org.komapper.annotation.KomapperUpdatedAt
import org.komapper.core.dsl.QueryDsl
import ru.descend.bot.catchToken
import ru.descend.bot.lolapi.LeagueApi
import ru.descend.bot.postgre.r2dbc.R2DBC
import ru.descend.bot.printLog
import java.time.LocalDateTime

@KomapperEntity
@KomapperTable("tbl_LOLs")
data class LOLs(
    @KomapperId
    @KomapperAutoIncrement
    val id: Int = 0,

    var LOL_puuid: String = "",
    var LOL_summonerId: String = "",
    var LOL_accountId: String = "",
    var LOL_summonerName: String = "",
    var LOL_riotIdName: String? = "",
    var LOL_riotIdTagline: String? = "",
    var LOL_region: String? = "",
    var LOL_summonerLevel: Int = 1,

    @KomapperCreatedAt
    val createdAt: LocalDateTime = LocalDateTime.MIN,
    @KomapperUpdatedAt
    val updatedAt: LocalDateTime = LocalDateTime.MIN
) {

    constructor(region: String, summonerName: String) : this() {
        val leagueApi = LeagueApi(catchToken()[1], region)
        leagueApi.leagueService.getBySummonerName(summonerName).execute().body()?.let {
            this.LOL_puuid = it.puuid
            this.LOL_summonerId = it.id
            this.LOL_accountId = it.accountId
            this.LOL_summonerName = it.name
            this.LOL_region = region
            this.LOL_summonerLevel = it.summonerLevel
        }
    }

    companion object {
        suspend fun addLOL(value: LOLs) : LOLs? {
            var result: LOLs? = null
            R2DBC.db.withTransaction {
                result = R2DBC.db.runQuery {
                    QueryDsl.insert(R2DBC.tbl_lols).single(value)
                }
                printLog("[R2DBC::addLOL] added lol id ${result?.id} with puuid ${result?.LOL_puuid}")
            }
            return result
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LOLs

        if (id != other.id) return false
        if (LOL_puuid != other.LOL_puuid) return false
        if (LOL_summonerId != other.LOL_summonerId) return false
        if (LOL_accountId != other.LOL_accountId) return false
        if (LOL_summonerName != other.LOL_summonerName) return false
        if (LOL_riotIdName != other.LOL_riotIdName) return false
        if (LOL_riotIdTagline != other.LOL_riotIdTagline) return false
        if (LOL_region != other.LOL_region) return false
        if (LOL_summonerLevel != other.LOL_summonerLevel) return false
        if (createdAt != other.createdAt) return false
        if (updatedAt != other.updatedAt) return false

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
        result = 31 * result + LOL_summonerLevel
        result = 31 * result + createdAt.hashCode()
        result = 31 * result + updatedAt.hashCode()
        return result
    }
}