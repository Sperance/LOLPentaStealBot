package ru.descend.bot.lolapi

import ru.descend.bot.catchToken

object LeagueMainObject {

    private val leagueApi = LeagueApi(catchToken()[1], LeagueApi.RU)

    var heroObjects = ArrayList<Any>()
    var heroNames = ArrayList<String>()
    fun catchHeroNames(): ArrayList<String> {
        val dragonService = leagueApi.dragonService

        val versions = dragonService.getVersions().execute().body()!!
        val champions = dragonService.getChampions(versions.first(), "ru_RU").execute().body()!!

        val namesAllHero = ArrayList<String>()
        champions.data::class.java.declaredFields.forEach {
            it.isAccessible = true
            val curData = it.get(champions.data)
            heroObjects.add(curData)
            val nameField = curData::class.java.getDeclaredField("name")
            nameField.isAccessible = true
            namesAllHero.add(nameField.get(curData).toString())
        }
        return namesAllHero
    }
}