package ru.descend.bot.postgre.tables

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
import ru.descend.bot.savedObj.isCurrentDay
import ru.descend.bot.sendMessage
import ru.descend.bot.to2Digits
import ru.descend.bot.toDate
import ru.descend.bot.toFormatDateTime
import save
import statements.select
import statements.selectAll
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
        var isSurrender = false
        match.info.participants.forEach {
            if (it.summonerId == "BOT" || it.puuid == "BOT") {
                isBots = true
            }
            if (it.gameEndedInEarlySurrender || it.teamEarlySurrendered) {
                isSurrender = true
            }
        }

        val pMatch: TableMatch
        val findMatchId = tableMatch.findIdOf { TableMatch::matchId eq match.metadata.matchId }
        if (findMatchId != null && findMatchId > 0){
            pMatch = TableMatch(
                id = findMatchId,
                matchId = match.metadata.matchId,
                matchDate = match.info.gameStartTimestamp,
                matchDateEnd = match.info.gameEndTimestamp,
                matchDuration = match.info.gameDuration,
                matchMode = match.info.gameMode,
                matchGameVersion = match.info.gameVersion,
                gameName = match.info.gameName,
                guild = this,
                bots = isBots,
                surrender = isSurrender
            )
        } else {
            pMatch = TableMatch(
                matchId = match.metadata.matchId,
                matchDate = match.info.gameStartTimestamp,
                matchDateEnd = match.info.gameEndTimestamp,
                matchDuration = match.info.gameDuration,
                matchMode = match.info.gameMode,
                matchGameVersion = match.info.gameVersion,
                gameName = match.info.gameName,
                guild = this,
                bots = isBots,
                surrender = isSurrender
            ).save()!!
        }

//        val findParticipants = tableParticipant.select().where { TableParticipant::guildUid eq guild.id.value.toString() }.where { TableParticipant::match eq pMatch}.getEntities()
//        if (findParticipants.isNotEmpty()){
//            printLog("[PostgreSQL Service] Match(${pMatch.id})${pMatch.matchId} already have ${findParticipants.size} participants")
//            return pMatch
//        }

        val arrayHeroName = ArrayList<Participant>()
        match.info.participants.forEach {part ->
            arrayHeroName.add(part)
        }

        match.info.participants.forEach {part ->
            var curLOL = tableLOLPerson.first { TableLOLPerson::LOL_puuid eq part.puuid }

            if (!isBots && curLOL != null) {
                if (part.pentaKills > 0 && (match.info.gameCreation.toDate().isCurrentDay() || match.info.gameEndTimestamp.toDate().isCurrentDay())) {
                    tableKORDLOL.first { TableKORD_LOL::LOLperson eq curLOL }?.let {
                        asyncLaunch {
                            val currentTeam = part.teamId
                            val textPentas = if (part.pentaKills == 1) "" else "(${part.pentaKills})"
                            guild.sendMessage(messageIdStatus, "Поздравляем!!!\n${it.asUser(guild).lowDescriptor()} cделал Пентакилл$textPentas за ${LeagueMainObject.findHeroForKey(part.championId.toString())} убив: ${arrayHeroName.filter { it.teamId != currentTeam }.joinToString { LeagueMainObject.findHeroForKey(it.championId.toString()) }}\nМатч: ${match.metadata.matchId} Дата: ${match.info.gameCreation.toFormatDateTime()}")
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
            }

            //Вдруг что изменится в профиле игрока
            if (curLOL.LOL_summonerName != part.summonerName || curLOL.LOL_riotIdTagline != part.riotIdTagline || curLOL.LOL_summonerId != part.summonerId) {
                curLOL.LOL_summonerName = part.summonerName
                curLOL.LOL_summonerId = part.summonerId
                curLOL.LOL_riotIdTagline = part.riotIdTagline
            }
            curLOL.save()

            TableParticipant(part, pMatch, curLOL).save()
        }

        if (findMatchId == null || findMatchId < 1){
            asyncLaunch {
                val arrayParts = tableParticipant.selectAll().where { TableParticipant::match eq pMatch }.getEntities()
                var textSending = "Добавлен ${pMatch.matchId}(${pMatch.id})Surrender:$isSurrender\nНачало: ${match.info.gameStartTimestamp.toFormatDateTime()} Конец: ${match.info.gameEndTimestamp.toFormatDateTime()}\nMode: ${match.info.gameMode} ${match.info.gameName}\n"
                if (!isSurrender) textSending += arrayParts.joinToString(separator = "\n") { "Summoner: ${it.LOLperson?.LOL_summonerName} Champion: ${LeagueMainObject.findHeroForKey(it.championId.toString())} MMR: ${it.getMMR()} Games: ${it.getCountForMatches()} KDA: ${it.kda.to2Digits()}" }
                guild.sendMessage(messageIdDebug, textSending)
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