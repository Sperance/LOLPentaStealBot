package ru.descend.bot.lolapi

import org.jetbrains.annotations.Nullable
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface LeagueService {

    /**
     * Get all champions mastery entries sorted by number of champion points descending
     *
     */
    @GET("/lol/champion-mastery/v3/champion-masteries/by-summoner/{summonerId}")
    fun getMasteries(@Path("summonerId") summonerId: Int): Call<List<ChampionMasteryDTO>>


    /**
     * Get a champion mastery by player ID and champion ID.
     *
     */
    @GET("/lol/champion-mastery/v3/champion-masteries/by-summoner/{summonerId}/by-champion/{championId}")
    fun getMasteries(
        @Path("summonerId") summonerId: Int,
        @Path("summonerId") championId: Int)
            : Call<ChampionMasteryDTO>


    /**
     * Get a player's total champion mastery score, which is the sum of individual champion mastery levels
     *
     */
    @GET("/lol/champion-mastery/v3/scores/by-summoner/{summonerId}")
    fun getChampionMasteryScore(@Path("summonerId") summonerId: Int): Call<Int>


    /**
     * Get the challenger league for given queue
     *
     */
    @GET("/lol/league/v3/challengerleagues/by-queue/{queue}")
    fun getChallengerLeagues(@Path("queue") queue: String) : Call<LeagueListDTO>


    @GET("/lol/platform/v3/champion-rotations")
    fun getChampionRotations() : Call<ChampionRotationsDTO>

    /**
     * Get league with given ID, including inactive entries
     *
     */
    @GET("/lol/league/v3/leagues/{leagueId}")
    fun getLeague(@Path("leagueId") leagueId: String) : Call<LeagueListDTO>

    /**
     * Get the master league for given queue
     *
     */
    @GET("/lol/league/v3/masterleagues/by-queue/{queue}")
    fun getMasterLeague(@Path("queue") queue: String) : Call<LeagueListDTO>

    /**
     * Get league positions in all queues for a given summoner ID
     *
     */
    @GET("/lol/league/v3/positions/by-summoner/{summonerId}")
    fun getLeaguePositions(@Path("summonerId") summonerId: Long) : Call<Set<LeaguePositionDTO>>


    /**
     * Get League of Legends status for the given shard
     *
     */
    @GET("/lol/status/v3/shard-data")
    fun getStatus() : Call<ShardStatus>


    /**
     * Get match by match ID
     *
     */
    @GET("/lol/match/v3/matches/{matchId}")
    fun getMatch(@Path("matchId") matchId: Long) : Call<MatchDTO>


    /**
     * Get matchlist for games played on given account ID and platform ID and
     * filtered using given filter parameters, if any
     */
    @GET("/lol/match/v3/matchlists/by-account/{accountId}")
    fun getMatchList(@Path("accountId") accountId: Long,
                     @Query("endTime") @Nullable endTime: Long,
                     @Query("beginIndex") @Nullable beginIndex: Int,
                     @Query("beginTime") @Nullable beginTime: Long,
                     @Query("champion") @Nullable champion: Set<Int>,
                     @Query("endIndex") @Nullable endIndex: Int,
                     @Query("queue") @Nullable queue: Set<Int>,
                     @Query("season") @Nullable season: Set<Int>)
            : Call<MatchlistDto>

    /**
     * Get match timeline by match ID
     *
     */
    @GET("/lol/match/v3/timelines/by-match/{matchId}")
    fun getMatchTimeline(@Path("matchId") matchId: Long) : Call<MatchTimelineDto>


    /**
     * Get match IDs by tournament code
     *
     */
    @GET("/lol/match/v3/matches/by-tournament-code/{tournamentCode}/ids")
    fun getMatchIds(@Path("tournamentCode") tournamentCode: String) : Call<List<Long>>


    /**
     * Get match by match ID and tournament code
     *
     */
    @GET("/lol/match/v3/matches/{matchId}/by-tournament-code/{tournamentCode}")
    fun getMatch(@Path("matchId") matchId: Long,
                 @Path("tournamentCode") tournamentCode: String)
            : Call<MatchDTO>

    /**
     * Get current game information for the given summoner ID
     *
     */
    @GET("/lol/spectator/v3/active-games/by-summoner/{summonerId}")
    fun getActiveGames(@Path("summonerId") summonerId: Long) : Call<CurrentGameInfo>


    /**
     * Get list of featured games
     *
     */
    @GET("/lol/spectator/v3/featured-games")
    fun getFeaturedGames() : Call<FeaturedGames>


    /**
     * Get a summoner by account ID
     *
     */
    @GET("/lol/summoner/v4/summoners/by-account/{accountId}")
    fun getSummonerByAccountId(@Path("accountId") accountId: Long) : Call<SummonerDto>


    /**
     * Get a summoner by summoner name
     *
     */
    @GET("/lol/summoner/v4/summoners/by-name/{summonerName}")
    fun getSummonerBySummonerName(@Path("summonerName") summonerName: String) : Call<SummonerDto>


    /**
     * Get a summoner by summoner ID
     *
     */
    @GET("/lol/summoner/v4/summoners/{summonerId}")
    fun getSummonerBySummonerId(@Path("summonerId") summonerId: String) : Call<SummonerDto>
}