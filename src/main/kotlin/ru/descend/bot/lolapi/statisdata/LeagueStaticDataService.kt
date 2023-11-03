package ru.descend.bot.lolapi.statisdata

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface LeagueStaticDataService {

    /**
     * Retrieve champion by ID
     *
     */
    @GET("/lol/static-data/v3/champions/{id}")
    fun getChampion(@Path("id") id: Long,
                    @Query("tags") tags: Set<String>,
                    @Query("locale") locale: String,
                    @Query("version") version: String,
                    @Query("champData") champData: Set<String>)
            : Call<ChampionDto>

    /**
     * Retrieves item list
     *
     */
    @GET("/lol/static-data/v3/items")
    fun getItems(@Query("tags") tags: Set<String>,
                 @Query("locale") locale: String,
                 @Query("version") version: String,
                 @Query("itemListData") itemListData: Set<String>)
            : Call<ItemListDto>


    /**
     * Retrieves item by ID
     *
     */
    @GET("/lol/static-data/v3/items/{id}")
    fun getItem(@Path("id") id: Long,
                @Query("tags") tags: Set<String>,
                @Query("locale") locale: String,
                @Query("version") version: String,
                @Query("itemData") itemData: Set<String>)
            : Call<ItemDto>


    /**
     * Retrieves language strings data
     *
     */
    @GET("/lol/static-data/v3/language-strings")
    fun getLanguageStrings(@Query("locale") locale: String,
                           @Query("version") version: String)
            : Call<LanguageStringsDto>


    /**
     * Retrieves supported languages data
     *
     */
    @GET("/lol/static-data/v3/languages")
    fun getLanguages() : Call<LanguageStringsDto>


    /**
     * Retrieves map data
     *
     */
    @GET("/lol/static-data/v3/maps")
    fun getMapData() : Call<MapDataDto>


    /**
     * Retrieves mastery list
     *
     */
    @GET("/lol/static-data/v3/masteries")
    fun getMasteries(@Query("tags") tags: Set<String>,
                     @Query("locale") locale: String,
                     @Query("version") version: String,
                     @Query("masteryListData") masteryListData: Set<String>)
            : Call<MasteryListDto>

    /**
     * Retrieves mastery item by ID
     *
     */
    @GET("/lol/static-data/v3/masteries/{id}")
    fun getMastery(@Path("id") id: Long,
                   @Query("tags") tags: Set<String>,
                   @Query("locale") locale: String,
                   @Query("version") version: String,
                   @Query("masteryData") masteryData: Set<String>)
            : Call<MasteryDto>

    /**
     * Retrieves profile icons
     *
     */
    @GET("/lol/static-data/v3/profile-icons")
    fun getProfileIcons(@Query("locale") locale: String,
                        @Query("version") version: String)
            : Call<ProfileIconDataDto>

    /**
     * Retrieves realm data
     *
     */
    @GET("/lol/static-data/v3/realms")
    fun getRealms() : Call<RealmDto>


    /**
     * Retrieves reforged rune path list
     *
     */
    @GET("/lol/static-data/v3/reforged-rune-paths")
    fun getReforgedRunesPathList(@Query("locale") locale: String,
                                 @Query("version") version: String)
            : Call<List<ReforgedRunePathDto>>

    /**
     * Retrieves reforged rune path by ID
     *
     */
    @GET("/lol/static-data/v3/reforged-rune-paths/{id}")
    fun getReforgedRunesPath(@Path("id") id: Int,
                             @Query("locale") locale: String,
                             @Query("version") version: String)
            : Call<ReforgedRunePathDto>

    /**
     * Retrieves reforged rune list
     *
     */
    @GET("/lol/static-data/v3/reforged-runes")
    fun getReforgedRunes(@Query("locale") locale: String,
                         @Query("version") version: String)
            : Call<List<ReforgedRuneDto>>

    /**
     * Retrieves reforged rune by ID
     *
     */
    @GET("/lol/static-data/v3/reforged-runes/{id}")
    fun getReforgedRune(@Path("id") id: Int,
                        @Query("locale") locale: String,
                        @Query("version") version: String)
            : Call<ReforgedRuneDto>

    /**
     * Retrieves rune list
     *
     */
    @GET("/lol/static-data/v3/runes")
    fun getRunes(@Query("tags") tags: Set<String>,
                 @Query("locale") locale: String,
                 @Query("version") version: String,
                 @Query("runeListData") runeData: Set<String>)
            : Call<RuneListDto>

    /**
     * Retrieves rune by ID
     *
     */
    @GET("/lol/static-data/v3/runes/{id}")
    fun getRune(@Query("tags") tags: Set<String>,
                @Query("locale") locale: String,
                @Query("version") version: String,
                @Query("runeData") runeData: Set<String>)
            : Call<RuneDto>


    /**
     * Retrieves summoner spell list
     *
     */
    @GET("/lol/static-data/v3/summoner-spells")
    fun getSummonerSpellList(@Query("tags") tags: Set<String>,
                             @Query("locale") locale: String,
                             @Query("version") version: String,
                             @Query("spellListData") spellListData: Set<String>)
            : Call<SummonerSpellListDto>

    /**
     * Retrieves summoner spell by ID
     *
     */
    @GET("/lol/static-data/v3/summoner-spells/{id}")
    fun getSummonerSpell(@Path("id") id: Long,
                         @Query("tags") tags: Set<String>,
                         @Query("locale") locale: String,
                         @Query("version") version: String,
                         @Query("spellData") spellData: Set<String>)
            : Call<SummonerSpellDto>

    /**
     * Retrieves full tarball link
     *
     */
    @GET("/lol/static-data/v3/tarball-links")
    fun getTarballLink(): Call<String>


    /**
     * Retrieves version data
     *
     */
    @GET("/lol/static-data/v3/versions")
    fun getVersions(): Call<List<String>>

}