package ru.descend.bot.lolapi

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import ru.descend.bot.lolapi.leaguedata.MatchTimelineDTO
import ru.descend.bot.lolapi.leaguedata.SummonerDTO
import ru.descend.bot.lolapi.leaguedata.championMasteryDto.ChampionMasteryDto
import ru.descend.bot.lolapi.leaguedata.currentGameInfo.CurrentGameInfo
import ru.descend.bot.lolapi.leaguedata.match_dto.MatchDTO

interface LeagueService {

    @GET("/lol/summoner/v4/summoners/by-name/{summonerName}")
    suspend fun getBySummonerName(@Path("summonerName") name: String) : Response<SummonerDTO>

    @GET("https://europe.api.riotgames.com/lol/match/v5/matches/by-puuid/{puuid}/ids")
    suspend fun getMatchIDByPUUID(@Path("puuid") puuid: String, @Query("start") start: Int, @Query("count") count: Int) : Response<List<String>>

    @GET("https://europe.api.riotgames.com/lol/match/v5/matches/{matchId}")
    suspend fun getMatchInfo(@Path("matchId") matchId: String) : Response<MatchDTO>

    @GET("https://europe.api.riotgames.com/lol/match/v5/matches/{matchId}/timeline")
    suspend fun getMatchTimeline(@Path("matchId") matchId: String) : Response<MatchTimelineDTO>

    @GET("/lol/champion-mastery/v4/champion-masteries/by-puuid/{puuid}/top")
    suspend fun getChampionMastery(@Path("puuid") puuid: String) : Response<ChampionMasteryDto>

    @GET("/lol/spectator/v4/active-games/by-summoner/{encryptedSummonerId}")
    suspend fun getActiveGame(@Path("encryptedSummonerId") encryptedSummonerId: String) : Response<CurrentGameInfo>

}