package ru.descend.bot.postgre

import Entity
import column
import databases.Database
import dev.kord.core.entity.Guild
import ru.descend.bot.asyncLaunch
import ru.descend.bot.lolapi.LeagueMainObject
import ru.descend.bot.lolapi.leaguedata.match_dto.MatchDTO
import ru.descend.bot.lolapi.leaguedata.match_dto.Participant
import ru.descend.bot.lowDescriptor
import ru.descend.bot.mainMapData
import ru.descend.bot.printLog
import ru.descend.bot.sendMessage
import ru.descend.bot.toFormatDateTime
import save
import table

data class TableGuild (
    override var id: Int = 0,
    var idGuild: String = "",
    var name: String = "",
    var applicationId: String? = null,
    var description: String = "",
    var ownerId: String = "",

    var botChannelId: String = "",
    var messageId: String = "",
    var messageIdStatus: String = "",
    var messageIdDebug: String = "",
    var messageIdPentaData: String = "",
    var messageIdGlobalStatisticData: String = "",
    var messageIdMasteryData: String = "",
    var messageIdRealTimeData: String = ""
): Entity() {

    val matches: List<TableMatch> by oneToMany(TableMatch::guild)
    val KORDusers: List<TableKORDPerson> by oneToMany(TableKORDPerson::guild)
    val KORDLOL: List<TableKORD_LOL> by oneToMany(TableKORD_LOL::guild)

    fun addMatch(guild: Guild, match: MatchDTO) : TableMatch {

        var isBots = false
        match.info.participants.forEach {
            if (it.summonerId == "BOT" || it.puuid == "BOT") {
                isBots = true
                return@forEach
            }
        }

        val pMatch = TableMatch(
            matchId = match.metadata.matchId,
            matchDate = match.info.gameCreation,
            matchDuration = match.info.gameDuration,
            matchMode = match.info.gameMode,
            matchGameVersion = match.info.gameVersion,
            gameName = match.info.gameName,
            guild = this,
            bots = isBots
        )
        pMatch.save()?.let {
            mainMapData[guild]?.addCurrentMatch(it)
        }

        if (match.info.participants.find { it.puuid == "BOT" || it.summonerId == "BOT" } != null) {
            printLog("[PostgreSQL Service] Creating Match(BOT) witg GUILD $idGuild with Match ${pMatch.matchId} ${pMatch.matchMode} time: ${pMatch.matchDate.toFormatDateTime()}")
            return pMatch
        }

        printLog("[PostgreSQL Service] Creating Match witg GUILD $idGuild with Match ${pMatch.matchId} ${pMatch.matchMode} time: ${pMatch.matchDate.toFormatDateTime()}")

        val arrayHeroName = ArrayList<Participant>()
        match.info.participants.forEach {part ->
            arrayHeroName.add(part)
        }

        match.info.participants.forEach {part ->
            var curLOL = tableLOLPerson.first { TableLOLPerson::LOL_puuid eq part.puuid }

            if (!isBots && curLOL != null) {
                if (part.pentaKills > 0) {
                    tableKORDLOL.first { TableKORD_LOL::LOLperson eq curLOL }?.let {
                        asyncLaunch {
                            val currentTeam = part.teamId
                            guild.sendMessage(messageIdStatus, "Поздравляем!!!\n${it.asUser(guild).lowDescriptor()} cделал Пентакилл за ${LeagueMainObject.findHeroForKey(part.championId.toString())} убив: ${arrayHeroName.filter { it.teamId != currentTeam }.joinToString { LeagueMainObject.findHeroForKey(it.championId.toString()) }}\nМатч: ${match.metadata.matchId} Дата: ${match.info.gameCreation.toFormatDateTime()}")
                        }
                    }
                }
            }

            var isNewPerson = false
            //Создаем нового игрока в БД
            if (curLOL == null) {
                curLOL = TableLOLPerson(
                    LOL_puuid = part.puuid,
                    LOL_summonerId = part.summonerId,
                    LOL_summonerName = part.summonerName,
                    LOL_riotIdName = part.riotIdName,
                    LOL_riotIdTagline = part.riotIdTagline)

                isNewPerson = true
                printLog("[PostgreSQL Service] Creating LOLPerson with PUUID ${part.puuid} NAME ${part.summonerName}")
            }

            //Вдруг что изменится в профиле игрока
            if (curLOL.LOL_summonerName != part.summonerName || curLOL.LOL_riotIdTagline != part.riotIdTagline || curLOL.LOL_summonerId != part.summonerId) {
                printLog("[PostgreSQL Service] Change LOLPerson with PUUID ${part.puuid} summonerName ${curLOL.LOL_summonerName} - ${part.summonerName} riotIdTagline ${curLOL.LOL_riotIdTagline} - ${part.riotIdTagline} summonerId ${curLOL.LOL_summonerId} - ${part.summonerId}")
                curLOL.LOL_summonerName = part.summonerName
                curLOL.LOL_summonerId = part.summonerId
                curLOL.LOL_riotIdTagline = part.riotIdTagline
            }
            curLOL.save()

            if (isNewPerson) {
                mainMapData[guild]?.addCurrentLOL(curLOL)
            }

            TableParticipant(part, pMatch, curLOL).save()?.let {
                mainMapData[guild]?.addCurrentParticipant(it)
            }
        }

        return pMatch
    }

    fun initGuild(guild: Guild) : TableGuild? {
        this.idGuild = guild.id.value.toString()
        this.name = guild.name
        this.applicationId = guild.applicationId?.value.toString()
        this.description = guild.description ?: ""
        this.ownerId = guild.ownerId.value.toString()
        return save()
    }
}

val tableGuild = table<TableGuild, Database> {
    column(TableGuild::idGuild).unique()
}