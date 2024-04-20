package ru.descend.bot.lolapi

import kotlinx.coroutines.delay
import ru.descend.bot.catchToken
import ru.descend.bot.globalLOLRequests
import ru.descend.bot.lolapi.champions.InterfaceChampionBase
import ru.descend.bot.lolapi.dto.MatchTimelineDTO
import ru.descend.bot.lolapi.dto.match_dto.MatchDTO
import ru.descend.bot.postgre.SQLData_R2DBC
import ru.descend.bot.printLog
import ru.descend.bot.statusLOLRequests
import ru.descend.bot.writeLog
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

object LeagueMainObject {

    private val leagueApi = LeagueApi(catchToken()[1], LeagueApi.RU)
    private val dragonService = leagueApi.dragonService
    val leagueService = leagueApi.leagueService

    private var heroObjects = ArrayList<Any>()

    var LOL_VERSION = ""
    var LOL_HEROES = 0

    fun catchHeroForId(id: String) : InterfaceChampionBase? {
        heroObjects.forEach {
            if (it is InterfaceChampionBase && it.key == id) return it
        }
        return null
    }

    suspend fun catchHeroNames(): ArrayList<String> {

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

        val namesAllHero = ArrayList<String>()
        heroObjects.clear()
        champions.data::class.java.declaredFields.forEach {
            it.isAccessible = true
            val curData = it.get(champions.data) as InterfaceChampionBase
            heroObjects.add(curData)
            val nameField = curData.name
            namesAllHero.add(nameField)
        }

        LOL_VERSION = champions.version
        LOL_HEROES = namesAllHero.size

        printLog("Version Data: ${champions.version} Heroes: ${namesAllHero.size}")

        return namesAllHero
    }

    suspend fun catchMatchID(puuid: String, summonerName: String, start: Int, count: Int) : List<String> {
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
                listOf()
            }
        }
    }

    suspend fun catchMatch(matchId: String) : MatchDTO? {
        globalLOLRequests++
        delay(checkRiotQuota())
        printLog("[catchMatch::$globalLOLRequests] started with matchId: $matchId")
        return when (val res = safeApiCall { reloadRiotQuota() ; leagueService.getMatchInfo(matchId) }){
            is Result.Success -> { res.data }
            is Result.Error -> {
                statusLOLRequests = 1
                val messageError = "catchMatch failure: ${res.message} with matchId: $matchId"
                printLog(messageError)
                writeLog(messageError)
                null
            }
        }
    }

    suspend fun catchPentaSteal(matchId: String) : MatchTimelineDTO? {
        globalLOLRequests++
        delay(checkRiotQuota())
        printLog("[catchPentaSteal::$globalLOLRequests] started with matchId: $matchId")
        return when (val res = safeApiCall { reloadRiotQuota() ; leagueService.getMatchTimeline(matchId) }){
            is Result.Success -> { res.data }
            is Result.Error -> {
                statusLOLRequests = 1
                val messageError = "catchPentaSteal failure: ${res.message} with matchId: $matchId"
                printLog(messageError)
                writeLog(messageError)
                null
            }
        }
    }

    /**
     * 20 запросов в 1 секунду
     * 100 запросов за 2 минуты
     */
    private fun checkRiotQuota(): kotlin.time.Duration {
        if (statusLOLRequests != 0 || globalLOLRequests >= 99) {
            statusLOLRequests = 1
            printLog("[leagueApi] checkRiotQuota globalLOLRequests: $globalLOLRequests")
            return ((2).minutes + (1).seconds) //+1 сек на всякий случай
        }
        return (0.1).seconds //для безопасности
    }

    private fun reloadRiotQuota() {
        if (statusLOLRequests == 1) {
            printLog("[leagueApi] reloadRiotQuota globalLOLRequests: $globalLOLRequests")
            statusLOLRequests = 0
            globalLOLRequests = 0
        }
    }

    fun findHeroForKey(key: String) : String {
        val returnObj = heroObjects.find { (it as InterfaceChampionBase).key == key } as InterfaceChampionBase?
        return returnObj?.name ?: "<Not Find>"
    }
}