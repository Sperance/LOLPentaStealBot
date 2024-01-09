package ru.descend.bot.lolapi

import ru.descend.bot.catchToken
import ru.descend.bot.lolapi.champions.InterfaceChampionBase
import ru.descend.bot.lolapi.leaguedata.championMasteryDto.ChampionMasteryDto
import ru.descend.bot.lolapi.leaguedata.match_dto.MatchDTO
import ru.descend.bot.printLog

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

    fun catchMatchID(puuid: String, start: Int, count: Int) : ArrayList<String> {
        val result = ArrayList<String>()
        return try {
            val exec = leagueService.getMatchIDByPUUID(puuid, start, count).execute()
            if (exec.isSuccessful) {
                exec.body()?.forEach {
                    result.add(it)
                }
            } else {
                printLog("catchMatchID failure: ${exec.code()} ${exec.message()}")
            }
            result
        }catch (_: Exception) {
            result
        }
    }

    fun catchMatch(matchId: String) : MatchDTO? {
        return try {
            val exec = leagueService.getMatchInfo(matchId).execute()
            if (!exec.isSuccessful) {
                printLog("catchMatch failure: ${exec.code()} ${exec.message()}")
            }
            exec.body()
        }catch (_: Exception) {
            null
        }
    }

    fun catchChampionMastery(puuid: String) : ChampionMasteryDto? {
        return leagueService.getChampionMastery(puuid).execute().body()
    }

    fun findHeroForKey(key: String) : String {
        val returnObj = heroObjects.find { (it as InterfaceChampionBase).key == key } as InterfaceChampionBase?
        return returnObj?.name ?: "<Not Find>"
    }
}