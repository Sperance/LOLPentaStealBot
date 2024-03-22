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
    var id: Int = 0,

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

    override suspend fun save() : LOLs {
        val result = R2DBC.db.withTransaction {
            R2DBC.db.runQuery { QueryDsl.insert(tbl_LOLs).single(this@LOLs) }
        }
        this.id = result.id
        this.updatedAt = result.updatedAt
        this.createdAt = result.createdAt
        printLog("[LOLs::save] $this")
        return this
    }

    override suspend fun update() : LOLs {
        val before = R2DBC.getLOLs { tbl_LOLs.id eq this@LOLs.id }.firstOrNull()
        printLog("[LOLs::update] $this { ${calculateUpdate(before, this)} }")
        return R2DBC.db.withTransaction {
            R2DBC.db.runQuery { QueryDsl.update(tbl_LOLs).single(this@LOLs) }
        }
    }

    override suspend fun delete() {
        printLog("[LOLs::delete] $this")
        R2DBC.db.withTransaction {
            R2DBC.db.runQuery { QueryDsl.delete(tbl_LOLs).single(this@LOLs) }
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

    override fun toString(): String {
        return "LOLs(id=$id, puuid='$LOL_puuid', summonerName='$LOL_summonerName', riotIdName=$LOL_riotIdName)"
    }
}