package ru.descend.bot.lolapi.services

import ru.descend.bot.lolapi.champions.ChampionsDTO
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface LLDragonService {

    @GET("https://ddragon.leagueoflegends.com/api/versions.json")
    suspend fun getVersions() : Response<List<String>>

    @GET("http://ddragon.leagueoflegends.com/cdn/{version}/data/{locale}/champion.json")
    suspend fun getChampions(@Path("version") version: String, @Path("locale") locale: String) : Response<ChampionsDTO>

}