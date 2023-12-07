package ru.descend.bot.firebase

import com.google.firebase.database.Exclude
import dev.kord.common.entity.Snowflake
import dev.kord.core.cache.data.UserData
import dev.kord.core.entity.Guild
import dev.kord.core.entity.User
import ru.descend.bot.lolapi.champions.InterfaceChampionBase
import ru.descend.bot.lolapi.leaguedata.match_dto.MatchDTO
import ru.descend.bot.lolapi.leaguedata.match_dto.Participant
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

data class FireParticipant(
    var championId: Int = -1,
    var summonerId: String = "",
    var championName: String = "",
    var summonerName: String = "",
    var puuid: String = "",
    var kills5: Int = 0,
    var kills4: Int = 0,
    var kills3: Int = 0,
    var kills2: Int = 0,
    var kills: Int = 0,
    var assists: Int = 0,
    var deaths: Int = 0,
    var goldEarned: Int = 0,
    var skillsCast: Int = 0,
    var totalDmgToChampions: Int = 0,
    var minionsKills: Int = 0,
    var team: Int = -1,
    var win: Boolean = false,

    @Exclude var statWins: Int = 0,
    @Exclude var statGames: Int = 0,
    @Exclude var sortIndex: Int = 0
) {

    @Exclude
    fun clearData() {
        kills5 = 0
        kills4 = 0
        kills3 = 0
        kills2 = 0
        kills = 0
        assists = 0
        deaths = 0
        goldEarned = 0
        skillsCast = 0
        totalDmgToChampions = 0
        minionsKills = 0

        statWins = 0
        statGames = 0
        sortIndex = 0
    }

    constructor(participant: Participant) : this() {
        val kill5 = participant.pentaKills
        val kill4 = participant.quadraKills - kill5
        val kill3 = participant.tripleKills - kill4
        val kill2 = participant.doubleKills - kill3

        this.championId = participant.championId
        this.summonerId = participant.summonerId
        this.championName = participant.championName
        this.summonerName = participant.summonerName
        this.puuid = participant.puuid
        this.kills5 = kill5
        this.kills4 = kill4
        this.kills3 = kill3
        this.kills2 = kill2
        this.kills = participant.kills
        this.assists = participant.assists
        this.deaths = participant.deaths
        this.goldEarned = participant.goldEarned
        this.skillsCast = participant.spell1Casts + participant.spell2Casts + participant.spell3Casts + participant.spell4Casts
        this.totalDmgToChampions = participant.totalDamageDealtToChampions
        this.minionsKills = participant.totalMinionsKilled
        this.team = participant.teamId
        this.win = participant.win
    }
}

data class FireMatch(
    var matchId: String = "",
    var matchDate: Long = 0,
    var matchDuration: Long = 0,
    var matchMode: String = "",
    var matchGameVersion: String = "",
    var listPerc: ArrayList<FireParticipant> = ArrayList(),
) : FireBaseData() {

    constructor(match: MatchDTO) : this() {
        this.matchId = match.metadata.matchId
        this.matchDate = match.info.gameCreation
        this.matchDuration = match.info.gameDuration.toLong()
        this.matchMode = match.info.gameMode
        this.matchGameVersion = match.info.gameVersion
        val list = ArrayList<FireParticipant>()
        match.info.participants.forEach { list.add(FireParticipant(it)) }
        this.listPerc = list
    }

    @Exclude
    fun getParticipants(persons: ArrayList<FirePerson>) : ArrayList<FirePerson> {
        val result = ArrayList<FirePerson>()
        listPerc.forEach { perc ->
            persons.find { it.LOL_puuid == perc.puuid }?.let { fp ->
                result.add(fp)
            }
        }
        return result
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