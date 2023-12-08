package ru.descend.bot.lolapi

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import ru.descend.bot.lolapi.leaguedata.SummonerDTO
import ru.descend.bot.lolapi.leaguedata.championMasteryDto.ChampionMasteryDto
import ru.descend.bot.lolapi.leaguedata.match_dto.MatchDTO

interface LeagueService {

    @GET("/lol/summoner/v4/summoners/by-name/{summonerName}")
    fun getBySummonerName(@Path("summonerName") name: String) : Call<SummonerDTO>

    @GET("https://europe.api.riotgames.com/lol/match/v5/matches/by-puuid/{puuid}/ids?start=0")
    fun getMatchIDByPUUID(@Path("puuid") puuid: String, @Query("count") count: Int) : Call<List<String>>

    @GET("https://europe.api.riotgames.com/lol/match/v5/matches/{matchId}")
    fun getMatchInfo(@Path("matchId") matchId: String) : Call<MatchDTO>

    @GET("/lol/champion-mastery/v4/champion-masteries/by-puuid/{puuid}/top")
    fun getChampionMastery(@Path("puuid") puuid: String) : Call<ChampionMasteryDto>

}