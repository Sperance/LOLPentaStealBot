package ru.descend.bot.lolapi

import kotlinx.coroutines.delay
import ru.descend.bot.catchToken
import ru.descend.bot.globalLOLRequests
import ru.descend.bot.lolapi.champions.InterfaceChampionBase
import ru.descend.bot.lolapi.dataclasses.SavedPartSteal
import ru.descend.bot.lolapi.leaguedata.championMasteryDto.ChampionMasteryDto
import ru.descend.bot.lolapi.leaguedata.currentGameInfo.CurrentGameInfo
import ru.descend.bot.lolapi.leaguedata.match_dto.MatchDTO
import ru.descend.bot.postgre.SQLData_R2DBC
import ru.descend.bot.postgre.r2dbc.model.Guilds
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

    suspend fun catchMatchID(sqldataR2dbc: SQLData_R2DBC, puuid: String, start: Int, count: Int) : ArrayList<String> {
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
        val exec = leagueService.getMatchInfo(matchId).execute()
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

    suspend fun catchPentaSteal(matchId: String) : ArrayList<Triple<String, String, String>> {

        globalLOLRequests++
        delay(checkRiotQuota())
        printLog("[catchPentaSteal::$globalLOLRequests] started with matchId: $matchId")

        val result = ArrayList<Triple<String, String, String>>()
        val exec = leagueService.getMatchTimeline(matchId).execute()

        if (!exec.isSuccessful) {
            statusLOLRequests = 1
            val messageError = "catchPentaSteal failure: ${exec.code()} ${exec.message()} with matchId: $matchId"
            printLog(messageError)
            return result
        }

        if (exec.body() == null) {
            statusLOLRequests = 1
            val messageError = "catchPentaSteal body failure: ${exec.code()} ${exec.message()} with matchId: $matchId"
            printLog(messageError)
            return result
        }

        exec.body()?.let {
            val mapPUUID = HashMap<Long, String>()
            it.info.participants.forEach { part ->
                mapPUUID[part.participantId] = part.puuid
            }

            var lastDate = System.currentTimeMillis()
            var removedPart: SavedPartSteal? = null
            var isNextCheck = false
            val arrayQuadras = ArrayList<SavedPartSteal>()
            var mainText = ""

            it.info.frames.forEach { frame ->
                frame.events.forEach lets@ { event ->
                    if (event.killerId != null && event.type.contains("CHAMPION")) {
                        val betw = Duration.between(lastDate.toDate().toInstant(), event.timestamp.toDate().toInstant())
                        val resDate = getStrongDate(event.timestamp)
                        lastDate = event.timestamp

                        val textLog = "EVENT: team:${if (event.killerId <= 5) "BLUE" else "RED"} killerId:${event.killerId} multiKillLength:${event.multiKillLength ?: 0} killType: ${event.killType?:""} type:${event.type} ${resDate.timeSec} STAMP: ${event.timestamp} BETsec: ${betw.toSeconds()}\n"
                        mainText += textLog

                        if (isNextCheck && (event.type == "CHAMPION_KILL" || event.type == "CHAMPION_SPECIAL_KILL")) {
                            arrayQuadras.forEach saved@ { sPart ->
                                if (sPart.team == (if (event.killerId <= 5) "BLUE" else "RED") && sPart.participantId != event.killerId) {
                                    printLog("PENTESTEAL. Чел PUUID ${mapPUUID[event.killerId]} состилил Пенту у ${sPart.puuid}")
                                    result.add(Triple(mapPUUID[event.killerId]!!, sPart.puuid, mainText))
                                    removedPart = sPart
                                    return@saved
                                }
                            }
                            if (removedPart != null) {
                                arrayQuadras.remove(removedPart)
                                removedPart = null
                            }
                            isNextCheck = false
                        }
                        if (event.multiKillLength == 4L) {
                            arrayQuadras.add(SavedPartSteal(event.killerId, mapPUUID[event.killerId] ?: "", if (event.killerId <= 5) "BLUE" else "RED", event.timestamp))
                            isNextCheck = true
                        }
                    }
                }
            }
        }

        return result
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