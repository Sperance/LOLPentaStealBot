package ru.descend.bot.lolapi

import ru.descend.bot.catchToken

object LeagueMainObject {

    private val leagueApi = LeagueApi(catchToken()[1], LeagueApi.RU)

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