package ru.descend.bot.postgre.r2dbc.model

import org.komapper.annotation.KomapperAutoIncrement
import org.komapper.annotation.KomapperEntity
import org.komapper.annotation.KomapperId
import org.komapper.annotation.KomapperTable
import org.komapper.core.dsl.Meta
import ru.descend.bot.lolapi.LeagueMainObject
import ru.descend.bot.lolapi.LeagueMainObject.LOL_VERSION
import ru.descend.bot.datas.Result
import ru.descend.bot.datas.safeApiCall
import ru.descend.bot.enums.EnumMMRRank
import ru.descend.bot.lolapi.dto.matchDto.Participant
import ru.descend.bot.printLog
import ru.descend.bot.statusLOLRequests
import ru.descend.bot.to1Digits
import kotlin.math.abs

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
    var mmrAram: Double = 0.0,
    var mmrAramSaved: Double = 0.0,
    var match_date_last: Long = 0,
) {
    companion object {
        val tbl_lols = Meta.loLs
    }

    /**
     * Минимальное снятие ММР при поражении (меньше этого значения - не снимется (без учёта Тегов))
     */
    fun calcMinRemovedMMR() : Double {
        return (1.0 + (getRank().rankValue / 2.0)).to1Digits()
    }

    /**
     * Максимальное снятие ММР при поражении (больше этого значения - не снимается (без учёта Тегов))
     */
    fun calcMaxRemovedMMR() : Double {
        return (getRank().rankValue + 1.0).to1Digits()
    }

    /**
     * Минимальное значение ММР получаемое при победе (меньше получить невозможно)
     */
    fun calcMinGainedMMR() : Double {
        return (1.0 + (getRank().rankValue / 2.0)).to1Digits()
    }

    fun removeMMRvalue(removedValue: Double) {
        if (mmrAramSaved > removedValue) {
            mmrAramSaved = (mmrAramSaved - removedValue).to1Digits()
        } else if (mmrAramSaved == removedValue) {
            mmrAramSaved = 0.0
        } else if (mmrAramSaved < removedValue) {
            val minusValue = abs(mmrAramSaved - removedValue)
            mmrAramSaved = 0.0
            mmrAram = (mmrAram - minusValue).to1Digits()
            if (mmrAram < 0.0) mmrAram = 0.0
        }
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
                null
            }
        }
    }

    fun isBot() = LOL_puuid.trim() == "BOT" || LOL_summonerId == "BOT" || LOL_puuid.length < 10

    fun isNeedUpdate(match: Matches, parts: Participant) : Boolean {
        if (match_date_last != 0L && match_date_last < match.matchDateEnd) {
            return isSameFields(match, parts)
        }
        if (match_date_last <= match.matchDateEnd && LOL_summonerLevel <= parts.summonerLevel) {
            return isSameFields(match, parts)
        }
        return false
    }

    private fun isSameFields(match: Matches, parts: Participant) : Boolean {
        return (LOL_region != match.getRegionValue() ||
            LOL_riotIdTagline != parts.riotIdTagline ||
            LOL_summonerId != parts.summonerId ||
            LOL_riotIdName != parts.riotIdGameName ||
            profile_icon != parts.profileIcon)
    }

    fun getIconURL() : String {
        return "https://ddragon.leagueoflegends.com/cdn/$LOL_VERSION/img/profileicon/$profile_icon.png"
    }

    fun getCorrectName() : String {
        return LOL_riotIdName?:""
    }

    fun getCorrectNameWithTag() : String {
        return getCorrectName() + "#" + LOL_riotIdTagline
    }

    fun getRank() = EnumMMRRank.getMMRRank(mmrAram)

    override fun toString(): String {
        return "LOLs(id=$id, riotIdName=${getCorrectName()}, region=$LOL_region, mmrAram=$mmrAram, savedAram=$mmrAramSaved)"
    }
}