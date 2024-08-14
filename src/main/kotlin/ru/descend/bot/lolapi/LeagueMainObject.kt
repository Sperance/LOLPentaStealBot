package ru.descend.bot.lolapi

import com.google.gson.Gson
import kotlinx.coroutines.delay
import ru.descend.bot.catchToken
import ru.descend.bot.datas.Result
import ru.descend.bot.datas.create
import ru.descend.bot.datas.safeApiCall
import ru.descend.bot.globalLOLRequests
import ru.descend.bot.lolapi.dto.InterfaceChampionBase
import ru.descend.bot.lolapi.dto.MatchTimelineDTO
import ru.descend.bot.lolapi.dto.championMasteryDto.ChampionMasteryDtoItem
import ru.descend.bot.lolapi.dto.matchDto.MatchDTO
import ru.descend.bot.postgre.PostgreTest.ChampionsDTOsample
import ru.descend.bot.postgre.R2DBC
import ru.descend.bot.postgre.r2dbc.model.Heroes
import ru.descend.bot.postgre.r2dbc.model.LOLs
import ru.descend.bot.printLog
import ru.descend.bot.statusLOLRequests
import ru.descend.bot.writeLog
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

object LeagueMainObject {

    private val leagueApi = LeagueApi(catchToken()[1], LeagueApi.RU)
    val dragonService = leagueApi.dragonService
    val leagueService = leagueApi.leagueService

    var LOL_VERSION = ""

    suspend fun catchHeroNames() {

        val versions = when (val res = safeApiCall { dragonService.getVersions() }){
            is Result.Success -> res.data
            is Result.Error -> {
                printLog("[catchHeroNames] error: ${res.message}")
                listOf()
            }
        }

        val champions = when (val res = safeApiCall { dragonService.getChampions(versions.first(), "ru_RU") }){
            is Result.Success -> res.data
            is Result.Error -> {
                printLog("[catchHeroNames] error: ${res.message}")
                throw IllegalAccessException("[catchHeroNames] error: ${res.message}")
            }
        }
        val heroes = R2DBC.stockHEROES.get()
        R2DBC.stockMMR.reset()
        val result = Gson().fromJson(Gson().toJson(champions), ChampionsDTOsample::class.java)
        result.data.forEach { (_, any2) ->
            val dataChamp = Gson().fromJson(Gson().toJson(any2), InterfaceChampionBase::class.java)
            if (heroes.find { hero -> hero.key == dataChamp.key } == null) {
                Heroes(nameEN = dataChamp.id, nameRU = dataChamp.name, key = dataChamp.key).create(Heroes::key)
            }
        }

        LOL_VERSION = champions.version
        printLog("Version Data: ${champions.version} Heroes: ${heroes.size}")
    }

    suspend fun catchMatchID(lol: LOLs, start: Int, count: Int, agained: Boolean = false) : List<String> {
        globalLOLRequests++
        delay(checkRiotQuota())
        printLog("[catchMatchID::$globalLOLRequests] started with summonerName: ${lol.getCorrectNameWithTag()}(lol_id: ${lol.id}) start: $start count: $count", writeToFile = false)
        return when (val res = safeApiCall { reloadRiotQuota() ; leagueService.getMatchIDByPUUID(lol.LOL_puuid, start, count) }){
            is Result.Success -> { res.data }
            is Result.Error -> {
                statusLOLRequests = 1
                val messageError = "catchMatchID failure: ${res.message} puuid: ${lol.LOL_puuid} start: $start count: $count"
                printLog(messageError)
                writeLog(messageError)

                if (agained) listOf()
                else catchMatchID(lol, start, count, true)
            }
        }
    }

    suspend fun catchChampionMasteries(puuid: String, region: String?, agained: Boolean = false) : List<ChampionMasteryDtoItem> {
        if (region == null) return listOf()
        globalLOLRequests++
        delay(checkRiotQuota())
        printLog("[catchChampionMasteries::$globalLOLRequests] started with puuid: $puuid region: $region")
        return when (val res = safeApiCall { reloadRiotQuota() ; leagueService.getChampionMasteryAny(puuid, region) }){
            is Result.Success -> { res.data }
            is Result.Error -> {
                val messageError = "catchChampionMasteries failure: ${res.message} puuid: $puuid region: $region"
                printLog(messageError)
                writeLog(messageError)

                if (res.errorCode == 404 || agained) listOf()
                else {
                    statusLOLRequests = 1
                    catchChampionMasteries(puuid, region)
                }
            }
        }
    }

//    suspend fun catchActiveGame(encryptedPUUID: String) : CurrentGameInfo? {
//        globalLOLRequests++
//        delay(checkRiotQuota())
//        printLog("[catchActiveGame::$globalLOLRequests] started with encryptedPUUID: $encryptedPUUID")
//        return when (val res = safeApiCall { reloadRiotQuota() ; leagueService.getActiveGame(encryptedPUUID) }){
//            is Result.Success -> { res.data }
//            is Result.Error -> {
//                null
//            }
//        }
//    }

    suspend fun catchMatch(matchId: String, agained: Boolean = false) : MatchDTO? {
        globalLOLRequests++
        delay(checkRiotQuota())
//        printLog("[catchMatch::$globalLOLRequests] started with matchId: $matchId")
        return when (val res = safeApiCall { reloadRiotQuota() ; leagueService.getMatchInfo(matchId) }){
            is Result.Success -> { res.data }
            is Result.Error -> {
                val messageError = "catchMatch failure: ${res.message} with matchId: $matchId"
                printLog(messageError)

                if (res.errorCode == 404 || agained) null
                else {
                    statusLOLRequests = 1
                    catchMatch(matchId)
                }
            }
        }
    }

    suspend fun catchPentaSteal(matchId: String, agained: Boolean = false) : MatchTimelineDTO? {
        globalLOLRequests++
        delay(checkRiotQuota())
//        printLog("[catchPentaSteal::$globalLOLRequests] started with matchId: $matchId")
        return when (val res = safeApiCall { reloadRiotQuota() ; leagueService.getMatchTimeline(matchId) }){
            is Result.Success -> { res.data }
            is Result.Error -> {
                val messageError = "catchPentaSteal failure: ${res.message} with matchId: $matchId"
                printLog(messageError)

                if (agained) null
                else {
                    statusLOLRequests = 1
                    catchPentaSteal(matchId)
                }
            }
        }
    }

    /**
     * 20 запросов в 1 секунду
     * 100 запросов за 2 минуты
     */
    private fun checkRiotQuota(): kotlin.time.Duration {
        if (statusLOLRequests != 0) {
            statusLOLRequests = 1
            return ((1).minutes)
        }
        return (0.1).seconds //для безопасности
    }

    private fun reloadRiotQuota() {
        if (statusLOLRequests == 1) {
            statusLOLRequests = 0
            globalLOLRequests = 0
        }
    }
}