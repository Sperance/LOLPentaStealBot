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
import ru.descend.bot.enums.EnumARAMRank
import ru.descend.bot.fromHexInt
import ru.descend.bot.lolapi.dto.matchDto.Participant
import ru.descend.bot.printLog
import ru.descend.bot.statusLOLRequests
import ru.descend.bot.to1Digits
import ru.descend.bot.toHexInt
import java.math.BigInteger
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

    var f_aram_games: Double = 0.0,
    var f_aram_wins: Double = 0.0,
    var f_aram_winstreak: Int = 0,
    var f_aram_kills: Double = 0.0,
    var f_aram_kills2: Double = 0.0,
    var f_aram_kills3: Double = 0.0,
    var f_aram_kills4: Double = 0.0,
    var f_aram_kills5: Double = 0.0,
    var f_aram_deaths: Double = 0.0,
    var f_aram_assists: Double = 0.0,
    var f_aram_last_key: BigInteger = BigInteger.ZERO,
    var f_aram_grades: BigInteger = BigInteger.ZERO,
    var f_aram_streaks: BigInteger = BigInteger.ZERO,
    var f_aram_roles: BigInteger = BigInteger.ZERO,
    var show_code: Int = 0
) {
    companion object {
        val tbl_lols = Meta.loLs
    }

    fun calculateFromParticipant(part: Participant, match: Matches?): LOLs {
        if (f_aram_grades == BigInteger.ZERO) f_aram_grades = "S:0;A:0;B:0;C:0;D:0;".toHexInt()
        if (f_aram_streaks == BigInteger.ZERO) f_aram_streaks = "W:0;L:0;".toHexInt()
        if (f_aram_roles == BigInteger.ZERO) f_aram_roles = "D:0;B:0;T:0;S:0;U:0;H:0;".toHexInt()

        if (match == null || match.matchMode != "ARAM") return this

        val kill5 = part.pentaKills
        val kill4 = part.quadraKills - kill5
        val kill3 = part.tripleKills - kill4
        val kill2 = part.doubleKills - kill3

        f_aram_games += 1
        f_aram_wins += if (part.win) 1 else 0
        f_aram_kills += part.kills
        f_aram_kills2 += kill2
        f_aram_kills3 += kill3
        f_aram_kills4 += kill4
        f_aram_kills5 += kill5
        f_aram_deaths += part.challenges?.deathsByEnemyChamps?:0
        f_aram_assists += part.assists
        if (f_aram_winstreak >= 0 && part.win) { f_aram_winstreak++ }
        else if (f_aram_winstreak <= 0 && part.win) f_aram_winstreak = 1
        else if (f_aram_winstreak >= 0) f_aram_winstreak = -1
        else { f_aram_winstreak-- }
        return this
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

    fun getIconURL() : String {
        return "https://ddragon.leagueoflegends.com/cdn/$LOL_VERSION/img/profileicon/$profile_icon.png"
    }

    fun getCorrectName() : String {
        return LOL_riotIdName?:""
    }

    fun getCorrectNameWithTag() : String {
        return getCorrectName() + "#" + LOL_riotIdTagline
    }

    fun countGrade(index: Int): Int {
        if (f_aram_grades == BigInteger.ZERO) return 0
        return f_aram_grades.fromHexInt().split(";")[index].split(":")[1].toIntOrNull() ?: 0
    }

    fun countStreak(index: Int): Int {
        if (f_aram_streaks == BigInteger.ZERO) return 0
        return f_aram_streaks.fromHexInt().split(";")[index].split(":")[1].toIntOrNull() ?: 0
    }

    fun countRoles(index: Int): Int {
        if (f_aram_roles == BigInteger.ZERO) return 0
        return f_aram_roles.fromHexInt().split(";")[index].split(":")[1].toIntOrNull() ?: 0
    }

    fun getARAMRank() = EnumARAMRank.getMMRRank(mmrAram)

    override fun toString(): String {
        return "LOLs(id=$id, riotIdName=${getCorrectNameWithTag()}, region=$LOL_region, mmrAram=$mmrAram, aram_games=$f_aram_games, grades=${f_aram_grades.fromHexInt()}, streaks=${f_aram_streaks.fromHexInt()}, roles=${f_aram_roles.fromHexInt()})"
    }
}