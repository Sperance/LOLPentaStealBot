package ru.descend.bot.lolapi

import kotlinx.coroutines.delay
import ru.descend.bot.catchToken
import ru.descend.bot.datas.Result
import ru.descend.bot.datas.create
import ru.descend.bot.datas.safeApiCall
import ru.descend.bot.globalLOLRequests
import ru.descend.bot.lolapi.dto.InterfaceChampionBase
import ru.descend.bot.lolapi.dto.MatchTimelineDTO
import ru.descend.bot.lolapi.dto.championMasteryDto.ChampionMasteryDtoItem
import ru.descend.bot.lolapi.dto.currentGameInfo.CurrentGameInfo
import ru.descend.bot.lolapi.dto.matchDto.MatchDTO
import ru.descend.bot.postgre.R2DBC
import ru.descend.bot.postgre.r2dbc.model.Heroes
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
        champions.data::class.java.declaredFields.forEach {
            it.isAccessible = true
            val curData = it.get(champions.data) as InterfaceChampionBase

            if (heroes.find { hero -> hero.key == curData.key } == null) {
                Heroes(nameEN = curData.id, nameRU = curData.name, key = curData.key).create(Heroes::key)
            }
        }

        LOL_VERSION = champions.version

        printLog("Version Data: ${champions.version} Heroes: ${heroes.size}")
    }

    suspend fun catchMatchID(puuid: String, summonerName: String, start: Int, count: Int, agained: Boolean = false) : List<String> {
        globalLOLRequests++
        delay(checkRiotQuota())
        printLog("[catchMatchID::$globalLOLRequests] started with summonerName: $summonerName start: $start count: $count")
        return when (val res = safeApiCall { reloadRiotQuota() ; leagueService.getMatchIDByPUUID(puuid, start, count) }){
            is Result.Success -> { res.data }
            is Result.Error -> {
                statusLOLRequests = 1
                val messageError = "catchMatchID failure: ${res.message} puuid: $puuid start: $start count: $count"
                printLog(messageError)
                writeLog(messageError)

                if (agained) listOf()
                else catchMatchID(puuid, summonerName,start, count, true)
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
        printLog("[catchMatch::$globalLOLRequests] started with matchId: $matchId")
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
        printLog("[catchPentaSteal::$globalLOLRequests] started with matchId: $matchId")
        return when (val res = safeApiCall { reloadRiotQuota() ; leagueService.getMatchTimeline(matchId) }){
            is Result.Success -> { res.data }
            is Result.Error -> {
                val messageError = "catchPentaSteal failure: ${res.message} with matchId: $matchId"
                printLog(messageError)
                writeLog(messageError)

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
            printLog("[leagueApi] checkRiotQuota globalLOLRequests: $globalLOLRequests")
            return ((1).minutes)
        }
        return (0).seconds //для безопасности
    }

    private fun reloadRiotQuota() {
        if (statusLOLRequests == 1) {
            printLog("[leagueApi] reloadRiotQuota globalLOLRequests: $globalLOLRequests")
            statusLOLRequests = 0
            globalLOLRequests = 0
        }
    }
}