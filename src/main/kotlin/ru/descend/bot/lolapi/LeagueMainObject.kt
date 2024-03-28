package ru.descend.bot.lolapi

import kotlinx.coroutines.delay
import ru.descend.bot.catchToken
import ru.descend.bot.globalLOLRequests
import ru.descend.bot.lolapi.champions.InterfaceChampionBase
import ru.descend.bot.lolapi.dataclasses.SavedPartSteal
import ru.descend.bot.lolapi.leaguedata.MatchTimelineDTO
import ru.descend.bot.lolapi.leaguedata.match_dto.MatchDTO
import ru.descend.bot.postgre.SQLData_R2DBC
import ru.descend.bot.printLog
import ru.descend.bot.savedObj.getStrongDate
import ru.descend.bot.statusLOLRequests
import ru.descend.bot.toDate
import java.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

object LeagueMainObject {

    private val leagueApi = LeagueApi(catchToken()[1], LeagueApi.RU)
    private val dragonService = leagueApi.dragonService
    private val leagueService = leagueApi.leagueService

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

        val versions = dragonService.getVersions().body()!!
        val champions = dragonService.getChampions(versions.first(), "ru_RU").body()!!

        val namesAllHero = ArrayList<String>()
        heroObjects.clear()
        champions.data::class.java.declaredFields.forEach {
            it.isAccessible = true
            val curData = it.get(champions.data)
            heroObjects.add(curData)
            val nameField = curData::class.java.getDeclaredField("name")
            nameField.isAccessible = true
            namesAllHero.add(nameField.get(curData).toString())
        }

        LOL_VERSION = champions.version
        LOL_HEROES = namesAllHero.size

        printLog("Version Data: ${champions.version} Heroes: ${namesAllHero.size}")

        return namesAllHero
    }

    suspend fun catchMatchID(sqldataR2dbc: SQLData_R2DBC, puuid: String, summonerName: String, start: Int, count: Int) : ArrayList<String> {
        val result = ArrayList<String>()
        globalLOLRequests++
        delay(checkRiotQuota())
        printLog("[catchMatchID::$globalLOLRequests] started with summonerName: $summonerName start: $start count: $count")
        try {
            val exec = leagueService.getMatchIDByPUUID(puuid, start, count)
            reloadRiotQuota()
            if (exec.isSuccessful) {
                exec.body()?.forEach {
                    result.add(it)
                }
            } else {
                statusLOLRequests = 1
                val messageError = "catchMatchID failure: ${exec.code()} ${exec.message()} puuid: $puuid start: $start count: $count"
                printLog(messageError)
                sqldataR2dbc.sendEmail("Error", messageError)
            }
        }catch (e: Exception) {
            statusLOLRequests = 1
            printLog("catchMatchID failure: puuid: $puuid start: $start count: $count error: ${e.localizedMessage}")
            return result
        }

        return result
    }

    suspend fun catchMatch(sqldataR2dbc: SQLData_R2DBC, matchId: String) : MatchDTO? {
        globalLOLRequests++
        delay(checkRiotQuota())
        printLog("[catchMatch::$globalLOLRequests] started with matchId: $matchId")
        val exec = leagueService.getMatchInfo(matchId)
        reloadRiotQuota()
        if (!exec.isSuccessful) {
            statusLOLRequests = 1
            val messageError = "catchMatch failure: ${exec.code()} ${exec.message()} with matchId: $matchId"
            printLog(messageError)
            sqldataR2dbc.sendEmail("Error", messageError)
            return null
        }
        return exec.body()
    }

    suspend fun catchPentaSteal(sqldataR2dbc: SQLData_R2DBC, matchId: String) : MatchTimelineDTO? {
        globalLOLRequests++
        delay(checkRiotQuota())
        printLog("[catchPentaSteal::$globalLOLRequests] started with matchId: $matchId")
        val exec = leagueService.getMatchTimeline(matchId)
        reloadRiotQuota()
        if (!exec.isSuccessful) {
            statusLOLRequests = 1
            val messageError = "catchPentaSteal failure: ${exec.code()} ${exec.message()} with matchId: $matchId"
            printLog(messageError)
            sqldataR2dbc.sendEmail("Error", messageError)
            return null
        }
        return exec.body()
    }

    /**
     * 20 запросов в 1 секунду
     * 100 запросов за 2 минуты
     */
    private fun checkRiotQuota(): kotlin.time.Duration {
        if (statusLOLRequests != 0 || globalLOLRequests >= 99) {
            statusLOLRequests = 1
            printLog("[leagueApi] checkRiotQuota globalLOLRequests: $globalLOLRequests")
            return ((2).minutes + (5).seconds) //+5 сек на всякий случай
        }
        return (0.2).seconds //для безопасности
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