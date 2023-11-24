package ru.descend.bot.firebase

import com.google.firebase.database.Exclude
import dev.kord.common.entity.Snowflake
import dev.kord.core.cache.data.UserData
import dev.kord.core.entity.Guild
import dev.kord.core.entity.User
import ru.descend.bot.lolapi.champions.InterfaceChampionBase
import ru.descend.bot.lolapi.leaguedata.match_dto.MatchDTO
import ru.descend.bot.printLog

data class FireKordPerson(
    var snowflake: String = "",
    var username: String = "",
    var discriminator: String = ""
) {
    @Exclude
    fun asUser(guild: Guild) : User {
        return User(UserData(Snowflake(snowflake.toLong()), username, discriminator), guild.kord)
    }

    companion object {
        @Exclude
        fun initKORD(user: User) : FireKordPerson{
            return FireKordPerson(
                snowflake = user.id.value.toString(),
                username = user.username,
                discriminator = user.discriminator
            )
        }
    }
}

data class FireMatch(
    var matchId: String = "",
    var hero: FireChampion? = null,
    var kills5: Int = 0,
    var kills4: Int = 0,
    var kills3: Int = 0,
    var kills2: Int = 0,
    var kills: Int = 0,
    var assists: Int = 0,
    var deaths: Int = 0,
    var matchDate: Long = 0,
    var matchDuration: Long = 0,
    var matchMode: String = "",
    var matchGameVersion: String = "",
    var listPerc: List<String>? = null,
    var win: Boolean = false
) : FireBaseData() {

    companion object {
        @Exclude
        fun initMatch(userUUID: String, match: MatchDTO) : FireMatch? {
            val champ = match.info.getCurrentParticipant(userUUID)
            if (champ == null) {
                printLog("Not find hero $userUUID in match ${match.metadata.matchId}")
                return null
            }
            val currentHero = FireChampion(key = champ.championId.toString(), name = champ.championName)
            val kill5 = champ.pentaKills
            val kill4 = champ.quadraKills - kill5
            val kill3 = champ.tripleKills - kill4
            val kill2 = champ.doubleKills - kill3
            return FireMatch(
                matchId = match.metadata.matchId,
                hero = currentHero,
                kills5 = kill5,
                kills4 = kill4,
                kills3 = kill3,
                kills2 = kill2,
                kills = champ.kills,
                matchDate = match.info.gameCreation,
                matchDuration = match.info.gameCreation,
                matchMode = match.info.gameMode,
                matchGameVersion = match.info.gameVersion,
                listPerc = match.metadata.participants,
                assists = champ.assists,
                deaths = champ.deaths,
                win = champ.win
            )
        }
    }
}

data class FirePSteal(
    var whoSteal: FireKordPerson? = null,
    var fromWhomSteal: FireKordPerson? = null,
    var hero: FireChampion? = null
) : FireBaseData()

data class FirePKill(
    var hero: FireChampion? = null,
    var match: String? = null
) : FireBaseData()

data class FireQKill(
    var match: String? = null,
    var hero: FireChampion? = null
) : FireBaseData()

data class FireChampion(
    var key: String = "",
    var name: String = "",
) {
    companion object {
        @Exclude
        fun catchFromDTO(champ: InterfaceChampionBase) : FireChampion {
            return FireChampion(
                key = champ.key,
                name = champ.name
            )
        }
    }
}