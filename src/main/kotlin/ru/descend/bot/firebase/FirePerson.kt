package ru.descend.bot.firebase

import com.google.firebase.database.Exclude
import dev.kord.common.entity.Snowflake
import dev.kord.core.cache.data.UserData
import dev.kord.core.entity.Guild
import dev.kord.core.entity.User
import ru.descend.bot.catchToken
import ru.descend.bot.lolapi.LeagueApi

data class FirePerson(
    var KORD_id: String = "",
    var KORD_name: String = "",
    var KORD_discriminator: String = "",

    var LOL_id: String = "",
    var LOL_accountId: String = "",
    var LOL_puuid: String = "",
    var LOL_name: String = "",
    var LOL_profileIconId: Int? = null,
    var LOL_summonerLevel: Int = 1,
    var LOL_region: String = ""
): FireBaseData() {

    @Exclude
    fun initKORD(user: User){
        this.KORD_id = user.id.value.toString()
        this.KORD_name = user.username
        this.KORD_discriminator = user.discriminator
    }

    @Exclude
    fun initLOL(region: String, summonerName: String) : CompleteResult {
        val leagueApi = LeagueApi(catchToken()[1], region)
        leagueApi.leagueService.getBySummonerName(summonerName).execute().body()?.let {
            this.LOL_id = it.id
            this.LOL_accountId = it.accountId
            this.LOL_puuid = it.puuid
            this.LOL_name = it.name
            this.LOL_profileIconId = it.profileIconId
            this.LOL_summonerLevel = it.summonerLevel
            this.LOL_region = region
            return CompleteResult.Success()
        }
        return CompleteResult.Error("Не найден призыватель $summonerName в регионе $region")
    }

    @Exclude
    fun asUser(guild: Guild) : User {
        return User(UserData(Snowflake(KORD_id.toLong()), KORD_name, KORD_discriminator), guild.kord)
    }
}