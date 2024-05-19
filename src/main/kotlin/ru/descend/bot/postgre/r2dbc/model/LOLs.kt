package ru.descend.bot.postgre.r2dbc.model

import org.komapper.annotation.KomapperAutoIncrement
import org.komapper.annotation.KomapperCreatedAt
import org.komapper.annotation.KomapperEntity
import org.komapper.annotation.KomapperId
import org.komapper.annotation.KomapperTable
import org.komapper.annotation.KomapperUpdatedAt
import org.komapper.core.dsl.Meta
import ru.descend.bot.lolapi.LeagueMainObject
import ru.descend.bot.lolapi.LeagueMainObject.LOL_VERSION
import ru.descend.bot.datas.Result
import ru.descend.bot.datas.safeApiCall
import ru.descend.bot.printLog
import ru.descend.bot.statusLOLRequests
import ru.descend.bot.writeLog
import java.time.LocalDateTime

@KomapperEntity
@KomapperTable("tbl_lols")
data class LOLs(
    @KomapperId
    @KomapperAutoIncrement
    val id: Int = 0,

    var LOL_puuid: String = "",
    var LOL_summonerId: String = "",
    var LOL_riotIdName: String? = "",
    var LOL_riotIdTagline: String? = "",
    var LOL_region: String? = "",
    var LOL_summonerLevel: Int = 1,
    var profile_icon: Int = 0,

    @KomapperCreatedAt
    var createdAt: LocalDateTime = LocalDateTime.MIN,
    @KomapperUpdatedAt
    var updatedAt: LocalDateTime = LocalDateTime.MIN
) {

    companion object {
        val tbl_lols = Meta.loLs
    }

    suspend fun connectLOL(region: String, summonerName: String, tagline: String) : LOLs? {
        return when (val res = safeApiCall { LeagueMainObject.leagueService.getByRiotNameWithTag(summonerName, tagline) }){
            is Result.Success -> {
                val newLOL = LOLs()
                newLOL.LOL_puuid = res.data.puuid
                newLOL.LOL_region = region
                newLOL.LOL_riotIdTagline = tagline
                newLOL.LOL_riotIdName = summonerName
                newLOL
            }
            is Result.Error -> {
                statusLOLRequests = 1
                val messageError = "connectLOL failure: ${res.message} with summonerName: $summonerName"
                printLog(messageError)
                writeLog(messageError)
                null
            }
        }
    }

    fun isBot() = LOL_puuid.trim() == "BOT"

    fun getIconURL() : String {
        return "https://ddragon.leagueoflegends.com/cdn/$LOL_VERSION/img/profileicon/$profile_icon.png"
    }

    fun getCorrectName() : String {
        return LOL_riotIdName?:""
    }

    fun getCorrectNameWithTag() : String {
        return getCorrectName() + "#" + LOL_riotIdTagline
    }

    override fun toString(): String {
        return "LOLs(id=$id, puuid='$LOL_puuid', riotIdName=$LOL_riotIdName)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LOLs

        if (id != other.id) return false
        if (LOL_puuid != other.LOL_puuid) return false
        if (LOL_summonerId != other.LOL_summonerId) return false
        if (LOL_riotIdName != other.LOL_riotIdName) return false
        if (LOL_riotIdTagline != other.LOL_riotIdTagline) return false
        if (LOL_region != other.LOL_region) return false
        if (LOL_summonerLevel != other.LOL_summonerLevel) return false
        if (profile_icon != other.profile_icon) return false
        if (createdAt != other.createdAt) return false
        if (updatedAt != other.updatedAt) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + LOL_puuid.hashCode()
        result = 31 * result + LOL_summonerId.hashCode()
        result = 31 * result + (LOL_riotIdName?.hashCode() ?: 0)
        result = 31 * result + (LOL_riotIdTagline?.hashCode() ?: 0)
        result = 31 * result + (LOL_region?.hashCode() ?: 0)
        result = 31 * result + LOL_summonerLevel
        result = 31 * result + profile_icon
        result = 31 * result + createdAt.hashCode()
        result = 31 * result + updatedAt.hashCode()
        return result
    }
}