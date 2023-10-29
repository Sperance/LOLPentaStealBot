package ru.descend.bot.lolapi

import ChampionsDTO

object LeagueMainObject {

    private val leagueApi = LeagueApi("RGAPI-a3c4d742-818d-40c9-9ec3-01fe2c426757", LeagueApi.RU)

    var heroObjects = ArrayList<Any>()
    var heroNames = ArrayList<String>()
    fun catchHeroNames(): ArrayList<String> {
        val exec = leagueApi.leagueService.getChampions()
        val body = exec.execute().body()!!
        val namesAllHero = ArrayList<String>()
        body.data::class.java.declaredFields.forEach {
            it.isAccessible = true
            val curData = it.get(body.data)
            heroObjects.add(curData)
            val nameField = curData::class.java.getDeclaredField("name")
            nameField.isAccessible = true
            namesAllHero.add(nameField.get(curData).toString())
        }
        return namesAllHero
    }
}