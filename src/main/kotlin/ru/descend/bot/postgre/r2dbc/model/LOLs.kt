package ru.descend.bot.postgre.r2dbc.model

import org.komapper.annotation.KomapperAutoIncrement
import org.komapper.annotation.KomapperCreatedAt
import org.komapper.annotation.KomapperEntity
import org.komapper.annotation.KomapperId
import org.komapper.annotation.KomapperTable
import org.komapper.annotation.KomapperUpdatedAt
import org.komapper.core.dsl.Meta
import org.komapper.core.dsl.QueryDsl
import ru.descend.bot.catchToken
import ru.descend.bot.lolapi.LeagueApi
import ru.descend.bot.postgre.r2dbc.R2DBC
import ru.descend.bot.postgre.r2dbc.interfaces.InterfaceR2DBC
import ru.descend.bot.printLog
import ru.descend.bot.savedObj.calculateUpdate
import java.time.LocalDateTime

val tbl_LOLs = Meta.loLs

@KomapperEntity
@KomapperTable("tbl_LOLs")
data class LOLs(
    @KomapperId
    @KomapperAutoIncrement
    val id: Int = 0,

    var oldID: Int = 0,
    var LOL_puuid: String = "",
    var LOL_summonerId: String = "",
    var LOL_accountId: String = "",
    var LOL_summonerName: String = "",
    var LOL_riotIdName: String? = "",
    var LOL_riotIdTagline: String? = "",
    var LOL_region: String? = "",
    var LOL_summonerLevel: Int = 1,

    @KomapperCreatedAt
    var createdAt: LocalDateTime = LocalDateTime.MIN,
    @KomapperUpdatedAt
    var updatedAt: LocalDateTime = LocalDateTime.MIN
) : InterfaceR2DBC<LOLs> {

    suspend fun connectLOL(region: String, summonerName: String) : LOLs? {
        val leagueApi = LeagueApi(catchToken()[1], region)
        val exec = leagueApi.leagueService.getBySummonerName(summonerName)
        if (!exec.isSuccessful) return null
        val newLOL = LOLs()
        exec.body()?.let {
            newLOL.LOL_puuid = it.puuid
            newLOL.LOL_summonerId = it.id
            newLOL.LOL_accountId = it.accountId
            newLOL.LOL_summonerName = it.name
            newLOL.LOL_region = region
            newLOL.LOL_summonerLevel = it.summonerLevel
        }
        return newLOL
    }

    override suspend fun save() : LOLs {
        val result = R2DBC.runQuery(QueryDsl.insert(tbl_LOLs).single(this@LOLs))
        printLog("[LOLs::save] $result")
        return result
    }

    override suspend fun update() : LOLs {
        val before = R2DBC.getLOLs { tbl_LOLs.id eq id }.firstOrNull()
        val after = R2DBC.runQuery(QueryDsl.update(tbl_LOLs).single(this@LOLs))
        printLog("[LOLs::update] $this { ${calculateUpdate(before, after)} }")
        return after
    }

    override suspend fun delete() {
        printLog("[LOLs::delete] $this")
        R2DBC.runQuery(QueryDsl.delete(tbl_LOLs).single(this@LOLs))
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

    override fun toString(): String {
        return "LOLs(id=$id, puuid='$LOL_puuid', summonerName='$LOL_summonerName', riotIdName=$LOL_riotIdName)"
    }
}