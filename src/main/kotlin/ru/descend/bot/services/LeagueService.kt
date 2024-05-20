package ru.descend.bot.services

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import ru.descend.bot.lolapi.dto.AccountDTO
import ru.descend.bot.lolapi.dto.MatchTimelineDTO
import ru.descend.bot.lolapi.dto.SummonerDTO
import ru.descend.bot.lolapi.dto.championMasteryDto.ChampionMasteryDto
import ru.descend.bot.lolapi.dto.currentGameInfo.CurrentGameInfo
import ru.descend.bot.lolapi.dto.match_dto.MatchDTO

interface LeagueService {
    @GET("https://europe.api.riotgames.com/riot/account/v1/accounts/by-riot-id/{gameName}/{tagLine}")
    suspend fun getByRiotNameWithTag(@Path("gameName") gameName: String, @Path("tagLine") tagLine: String) : Response<AccountDTO>

    @GET("https://europe.api.riotgames.com/lol/match/v5/matches/by-puuid/{puuid}/ids")
    suspend fun getMatchIDByPUUID(@Path("puuid") puuid: String, @Query("start") start: Int, @Query("count") count: Int) : Response<List<String>>

    @GET("https://europe.api.riotgames.com/lol/match/v5/matches/{matchId}")
    suspend fun getMatchInfo(@Path("matchId") matchId: String) : Response<MatchDTO>

    @GET("https://europe.api.riotgames.com/lol/match/v5/matches/{matchId}/timeline")
    suspend fun getMatchTimeline(@Path("matchId") matchId: String) : Response<MatchTimelineDTO>

    @GET("/lol/champion-mastery/v4/champion-masteries/by-puuid/{puuid}/top")
    suspend fun getChampionMastery(@Path("puuid") puuid: String) : Response<ChampionMasteryDto>

    @GET("/lol/spectator/v5/active-games/by-summoner/{encryptedPUUID}")
    suspend fun getActiveGame(@Path("encryptedPUUID") encryptedPUUID: String) : Response<CurrentGameInfo>
}