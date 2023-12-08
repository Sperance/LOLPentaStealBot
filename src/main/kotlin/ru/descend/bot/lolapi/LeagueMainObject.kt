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

    var heroObjects = ArrayList<Any>()
    var heroNames = ArrayList<String>()

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

    fun catchMatchID(puuid: String, count: Int) : ArrayList<String> {
        val result = ArrayList<String>()
        leagueService.getMatchIDByPUUID(puuid, count).execute().body()?.forEach {
            result.add(it)
        }
        return result
    }

    fun catchMatch(matchId: String) : MatchDTO? {
        return leagueService.getMatchInfo(matchId).execute().body()
    }

    fun catchChampionMastery(puuid: String) : ChampionMasteryDto? {
        return leagueService.getChampionMastery(puuid).execute().body()
    }

    fun findHeroForKey(key: String) : InterfaceChampionBase {
         return heroObjects.find { (it as InterfaceChampionBase).key == key } as InterfaceChampionBase
    }

    fun findHeroForName(name: String) : InterfaceChampionBase {
         return heroObjects.find { (it as InterfaceChampionBase).name == name } as InterfaceChampionBase
    }
}