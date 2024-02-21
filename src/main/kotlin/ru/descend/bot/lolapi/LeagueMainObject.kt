package ru.descend.bot.lolapi

import kotlinx.coroutines.delay
import ru.descend.bot.catchToken
import ru.descend.bot.globalLOLRequests
import ru.descend.bot.lolapi.champions.InterfaceChampionBase
import ru.descend.bot.lolapi.leaguedata.championMasteryDto.ChampionMasteryDto
import ru.descend.bot.lolapi.leaguedata.currentGameInfo.CurrentGameInfo
import ru.descend.bot.lolapi.leaguedata.match_dto.MatchDTO
import ru.descend.bot.postgre.tables.TableGuild
import ru.descend.bot.printLog
import ru.descend.bot.statusLOLRequests
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

    suspend fun catchMatchID(guild: TableGuild, puuid: String, start: Int, count: Int) : ArrayList<String> {
        val result = ArrayList<String>()
        globalLOLRequests++
        delay(checkRiotQuota())
        printLog("[catchMatchID::$globalLOLRequests] started with puuid: $puuid start: $start count: $count")
        try {
            val exec = leagueService.getMatchIDByPUUID(puuid, start, count).execute()
            reloadRiotQuota()
            if (exec.isSuccessful) {
                exec.body()?.forEach {
                    result.add(it)
                }
            } else {
                statusLOLRequests = 1
                val messageError = "catchMatchID failure: ${exec.code()} ${exec.message()} puuid: $puuid start: $start count: $count"
                printLog(messageError)
                guild.sendEmail(messageError)
            }
        }catch (e: Exception) {
            statusLOLRequests = 1
            printLog("catchMatchID failure: puuid: $puuid start: $start count: $count error: ${e.localizedMessage}")
            return result
        }

        return result
    }

    suspend fun catchMatch(guild: TableGuild, matchId: String) : MatchDTO? {
        globalLOLRequests++
        delay(checkRiotQuota())
        printLog("[catchMatch::$globalLOLRequests] started with matchId: $matchId")
        val exec = leagueService.getMatchInfo(matchId).execute()
        reloadRiotQuota()
        if (!exec.isSuccessful) {
            statusLOLRequests = 1
            val messageError = "catchMatch failure: ${exec.code()} ${exec.message()} with matchId: $matchId"
            printLog(messageError)
            guild.sendEmail(messageError)
            return null
        }
        return exec.body()
    }

    suspend fun catchChampionMastery(guild: TableGuild, puuid: String) : ChampionMasteryDto? {
        globalLOLRequests++
        delay(checkRiotQuota())
        printLog("[catchChampionMastery::$globalLOLRequests] started with puuid: $puuid")
        val exec = leagueService.getChampionMastery(puuid).execute()
        reloadRiotQuota()
        if (!exec.isSuccessful){
            statusLOLRequests = 1
            val messageError = "catchChampionMastery failure: ${exec.code()} ${exec.message()} with puuid: $puuid"
            printLog(messageError)
            guild.sendEmail(messageError)
            return null
        }
        return exec.body()
    }

    suspend fun catchActiveGame(guild: TableGuild, encryptedSummonerId: String) : CurrentGameInfo? {
        globalLOLRequests++
        delay(checkRiotQuota())
        printLog("[catchActiveGame::$globalLOLRequests] started with encryptedSummonerId: $encryptedSummonerId")
        val exec = leagueService.getActiveGame(encryptedSummonerId).execute()
        reloadRiotQuota()
        if (!exec.isSuccessful){
            statusLOLRequests = 1
            val messageError = "catchActiveGame failure: ${exec.code()} ${exec.message()} with encryptedSummonerId: $encryptedSummonerId"
            printLog(messageError)
            guild.sendEmail(messageError)
            return null
        }
        if (exec.code() == 404 || exec.message() == "Data not found - spectator game info isn't found"){
            guild.sendEmail("catchActiveGame failure: ${exec.code()} ${exec.message()} with encryptedSummonerId: $encryptedSummonerId")
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