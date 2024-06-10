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
    var last_loaded: Long = 0,
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
}