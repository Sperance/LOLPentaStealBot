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
import ru.descend.bot.postgre.R2DBC
import ru.descend.bot.postgre.r2dbc.model.Heroes
import ru.descend.bot.postgre.r2dbc.model.LOLs
import ru.descend.bot.printLog
import ru.descend.bot.statusLOLRequests
import ru.descend.bot.writeLog
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

data class ChampionsDTOsample(
    val type: String,
    val format: String,
    val version: String,
    val data: HashMap<Any, Any>,
)

object LeagueMainObject {

    private val leagueApi = LeagueApi(catchToken()[1], LeagueApi.RU)
    val dragonService = leagueApi.dragonService
    val leagueService = leagueApi.leagueService

    var LOL_VERSION = ""

    suspend fun catchHeroNames() {

        printLog("[catchHeroNames] catch versions")
        val versions = when (val res = safeApiCall { dragonService.getVersions() }){
            is Result.Success -> res.data
            is Result.Error -> {
                printLog("[catchHeroNames] error: ${res.message}")
                listOf()
            }
        }

        printLog("[catchHeroNames] catch champions. version: ${versions.first()}")
        val champions = when (val res = safeApiCall { dragonService.getChampions(versions.first(), "ru_RU") }){
            is Result.Success -> res.data
            is Result.Error -> {
                printLog("[catchHeroNames] error: ${res.message}")
                throw IllegalAccessException("[catchHeroNames] error: ${res.message}")
            }
        }
        val heroes = R2DBC.stockHEROES.get()
        val result = Gson().fromJson(Gson().toJson(champions), ChampionsDTOsample::class.java)
        result.data.forEach { (_, any2) ->
            val dataChamp = Gson().fromJson(Gson().toJson(any2), InterfaceChampionBase::class.java)
            if (heroes.find { hero -> hero.key == dataChamp.key } == null) {
                printLog("Create new champion: $dataChamp")
                val resultHero = Heroes(nameEN = dataChamp.id, nameRU = dataChamp.name, key = dataChamp.key).create(Heroes::key)
                R2DBC.stockHEROES.add(resultHero.result)
            }
        }

        LOL_VERSION = champions.version
        printLog("Version Data: ${champions.version} Heroes: ${heroes.size}")
    }

    suspend fun catchMatchID(lol: LOLs, start: Int, count: Int, agained: Boolean = false) : List<String> {
        globalLOLRequests++
        delay(checkRiotQuota())
        printLog("[catchMatchID::$globalLOLRequests] started with summonerName: ${lol.getCorrectNameWithTag()}(lol_id: ${lol.id}) start: $start count: $count puuid: ${lol.LOL_puuid}", writeToFile = false)
        return when (val res = safeApiCall { reloadRiotQuota() ; leagueService.getMatchIDByPUUID(lol.LOL_puuid, start, count) }){
            is Result.Success -> { res.data }
            is Result.Error -> {
                statusLOLRequests = 1
                val messageError = "catchMatchID failure: ${res.message} lol_id: ${lol.id} puuid: ${lol.LOL_puuid}"
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

    suspend fun catchMatch(matchId: String, agained: Boolean = false) : MatchDTO? {
        globalLOLRequests++
        delay(checkRiotQuota())
        return when (val res = safeApiCall { reloadRiotQuota() ; leagueService.getMatchInfo(matchId) }){
            is Result.Success -> { res.data }
            is Result.Error -> {
                val messageError = "catchMatch failure: ${res.message} with matchId: $matchId"
                printLog(messageError)

                if ((res.errorCode == 403 || res.errorCode == 404 || res.errorCode == 429) || agained) null
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
        return (0.0).seconds //для безопасности
    }

    private fun reloadRiotQuota() {
        if (statusLOLRequests == 1) {
            statusLOLRequests = 0
            globalLOLRequests = 0
        }
    }
}