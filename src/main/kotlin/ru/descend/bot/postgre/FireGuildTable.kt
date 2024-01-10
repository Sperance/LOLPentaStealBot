package ru.descend.bot.postgre

import Entity
import column
import databases.Database
import dev.kord.core.entity.Guild
import ru.descend.bot.firebase.FireMatch
import ru.descend.bot.firebase.FirePerson
import save
import table
import update

data class FireGuildTable (
    override var id: Int = 0,
    var idGuild: String = "",
    var name: String = "",
    var applicationId: String? = null,
    var description: String = "",
    var ownerId: String = "",

    var botChannelId: String = "",
    var messageId: String = "",
    var messageIdPentaData: String = "",
    var messageIdGlobalStatisticData: String = "",
    var messageIdMasteryData: String = "",
): Entity() {

    val matches: List<FireMatchTable> by oneToMany(FireMatchTable::guild)

    companion object {
        fun getForId(id: Int) : FireGuildTable? {
            return fireGuildTable.first { FireGuildTable::id eq id }
        }
    }

    fun addMatchFire(it: FireMatch) {

        if (matches.find { mat -> mat.matchId == it.matchId } != null) return

        val pMatch = FireMatchTable(
            matchId = it.matchId,
            matchDate = it.matchDate,
            matchDuration = it.matchDuration,
            matchMode = it.matchMode,
            matchGameVersion = it.matchGameVersion,
            gameName = it.gameName,
            guild = this
        )
        pMatch.save()

        it.listPerc.forEach {part ->
            var curLOL = fireLOLPersonTable.first { FireLOLPersonTable::LOL_puuid eq part.puuid }

            //Создаем нового игрока в БД
            if (curLOL == null) {
                curLOL = FireLOLPersonTable(
                    LOL_puuid = part.puuid,
                    LOL_summonerId = part.summonerId,
                    LOL_summonerName = part.summonerName,
                    LOL_riotIdName = part.riotIdName,
                    LOL_riotIdTagline = part.riotIdTagline)
            }

            //Вдруг что изменится в профиле игрока
            if ((curLOL.LOL_riotIdName.isNullOrEmpty() && curLOL.LOL_riotIdTagline.isNullOrEmpty()) || curLOL.LOL_summonerName != part.summonerName || curLOL.LOL_riotIdTagline != part.riotIdTagline) {
                curLOL.LOL_summonerName = part.summonerName
                curLOL.LOL_riotIdName = part.riotIdName
                curLOL.LOL_riotIdTagline = part.riotIdTagline
            }
            curLOL.save()

            FireParticipantTable(
                championId = part.championId,
                championName = part.championName,
                kills5 = part.kills5.toLong(),
                kills4 = part.kills4.toLong(),
                kills3 = part.kills3.toLong(),
                kills2 = part.kills2.toLong(),
                kills = part.kills.toLong(),
                assists = part.assists.toLong(),
                deaths = part.deaths.toLong(),
                goldEarned = part.goldEarned.toLong(),
                skillsCast = part.skillsCast.toLong(),
                totalDmgToChampions = part.totalDmgToChampions.toLong(),
                minionsKills = part.minionsKills.toLong(),
                team = part.team,
                win = part.win,
                match = pMatch,
                LOLperson = curLOL
            ).save()
        }
    }

    fun initGuild(guild: Guild) {
        this.idGuild = guild.id.value.toString()
        this.name = guild.name
        this.applicationId = guild.applicationId?.value.toString()
        this.description = guild.description ?: ""
        this.ownerId = guild.ownerId.value.toString()
    }
}

val fireGuildTable = table<FireGuildTable, Database> {
    column(FireGuildTable::idGuild).unique()
}