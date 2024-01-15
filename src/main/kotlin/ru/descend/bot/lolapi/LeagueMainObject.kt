package ru.descend.bot.lolapi

import kotlinx.coroutines.delay
import ru.descend.bot.catchToken
import ru.descend.bot.globalLOLRequests
import ru.descend.bot.launch
import ru.descend.bot.lolapi.champions.InterfaceChampionBase
import ru.descend.bot.lolapi.leaguedata.championMasteryDto.ChampionMasteryDto
import ru.descend.bot.lolapi.leaguedata.match_dto.MatchDTO
import ru.descend.bot.printLog
import ru.descend.bot.statusLOLRequests
import java.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

object LeagueMainObject {

    private val leagueApi = LeagueApi(catchToken()[1], LeagueApi.RU)
    private val dragonService = leagueApi.dragonService
    private val leagueService = leagueApi.leagueService

    private var heroObjects = ArrayList<Any>()

    var LOL_VERSION = ""
    var LOL_HEROES = 0

    fun catchHeroNames(): ArrayList<String> {

        val versions = dragonService.getVersions().execute().body()!!
        val champions = dragonService.getChampions(versions.first(), "ru_RU").execute().body()!!

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

    suspend fun catchMatchID(puuid: String, start: Int, count: Int) : ArrayList<String> {
        val result = ArrayList<String>()
        globalLOLRequests++
        delay(checkRiotQuota())
        printLog("[catchMatchID::$globalLOLRequests] started with puuid: $puuid start: $start count: $count")
        val exec = leagueService.getMatchIDByPUUID(puuid, start, count).execute()
        reloadRiotQuota()
        if (exec.isSuccessful) {
            exec.body()?.forEach {
                result.add(it)
            }
        } else {
            statusLOLRequests = 1
            printLog("catchMatchID failure: ${exec.code()} ${exec.message()}")
        }
        return result
    }

    suspend fun catchMatch(matchId: String) : MatchDTO? {
        globalLOLRequests++
        delay(checkRiotQuota())
        printLog("[catchMatch::$globalLOLRequests] started with matchId: $matchId")
        val exec = leagueService.getMatchInfo(matchId).execute()
        reloadRiotQuota()
        if (!exec.isSuccessful) {
            statusLOLRequests = 1
            printLog("catchMatch failure: ${exec.code()} ${exec.message()}")
        }
        return exec.body()
    }

    suspend fun catchChampionMastery(puuid: String) : ChampionMasteryDto? {
        globalLOLRequests++
        delay(checkRiotQuota())
        printLog("[catchChampionMastery::$globalLOLRequests] started with puuid: $puuid")
        val exec = leagueService.getChampionMastery(puuid).execute()
        reloadRiotQuota()
        if (!exec.isSuccessful){
            statusLOLRequests = 1
            printLog("catchChampionMastery failure: ${exec.code()} ${exec.message()}")
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
        return (10).milliseconds //для безопасности
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