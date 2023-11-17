package ru.descend.bot.lolapi

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import ru.descend.bot.lolapi.leaguedata.SummonerDTO

interface LeagueService {

    @GET("/lol/summoner/v4/summoners/by-name/{summonerName}")
    fun getBySummonerName(@Path("summonerName") name: String) : Call<SummonerDTO>

}