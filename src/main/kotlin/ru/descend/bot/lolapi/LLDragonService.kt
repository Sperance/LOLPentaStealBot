package ru.descend.bot.lolapi

import ru.descend.bot.lolapi.champions.ChampionsDTO
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import ru.descend.bot.lolapi.champions.DragonVersionDTO

interface LLDragonService {

    @GET("https://ddragon.leagueoflegends.com/api/versions.json")
    fun getVersions() : Call<List<String>>

    @GET("http://ddragon.leagueoflegends.com/cdn/{version}/data/{locale}/champion.json")
    fun getChampions(@Path("version") version: String, @Path("locale") locale: String) : Call<ChampionsDTO>

}