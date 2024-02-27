package ru.descend.bot.postgre.tables

import Entity
import column
import databases.Database
import dev.kord.core.entity.Guild
import kotlinx.coroutines.delay
import ru.descend.bot.asyncLaunch
import ru.descend.bot.lolapi.LeagueMainObject
import ru.descend.bot.lolapi.leaguedata.match_dto.MatchDTO
import ru.descend.bot.lolapi.leaguedata.match_dto.Participant
import ru.descend.bot.lowDescriptor
import ru.descend.bot.mail.GMailSender
import ru.descend.bot.mainMapData
import ru.descend.bot.postgre.SQLData
import ru.descend.bot.postgre.execProcedure
import ru.descend.bot.printLog
import ru.descend.bot.savedObj.CalculateMMR
import ru.descend.bot.savedObj.isCurrentDay
import ru.descend.bot.sendMessage
import ru.descend.bot.to2Digits
import ru.descend.bot.toDate
import ru.descend.bot.toFormatDateTime
import save
import statements.select
import statements.selectAll
import table
import update

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
    var messageIdMain: String = "",
    var messageIdDebug: String = "",
    var messageIdPentaData: String = "",
    var messageIdGlobalStatisticData: String = "",
    var messageIdMasteryData: String = "",
    var messageIdRealTimeData: String = "",
    var messageIdArammmr: String = ""
): Entity() {

    val matches: List<TableMatch> by oneToMany(TableMatch::guild)
    val KORDusers: List<TableKORDPerson> by oneToMany(TableKORDPerson::guild)
    val KORDLOL: List<TableKORD_LOL> by oneToMany(TableKORD_LOL::guild)

    suspend fun addMatch(sqlData: SQLData, match: MatchDTO, kordLol: List<TableKORD_LOL>? = null, tableMMR: List<TableMmr>? = null) : TableMatch {

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

        if (pMatch.id % 1000 == 0){
            asyncLaunch {
                sendEmail("execute method GetAVGs()")
                execProcedure("call \"GetAVGs\"()")
            }
        }

        val arrayHeroName = ArrayList<Participant>()
        match.info.participants.forEach {part ->
            arrayHeroName.add(part)
        }

        match.info.participants.forEach {part ->
            var curLOL = tableLOLPerson.first { TableLOLPerson::LOL_puuid eq part.puuid }

            if (kordLol != null && tableMMR != null && curLOL != null && !isBots && !isSurrender){
                kordLol.find { it.LOLperson?.id == curLOL?.id }?.let {
                    if (part.pentaKills > 0 || (part.quadraKills - part.pentaKills) > 0) {
                        asyncLaunch {
                            if (part.pentaKills > 0 && (match.info.gameCreation.toDate().isCurrentDay() || match.info.gameEndTimestamp.toDate().isCurrentDay())) {
                                val textPentas = if (part.pentaKills == 1) "" else "(${part.pentaKills})"
                                sqlData.guild.sendMessage(messageIdStatus, "Поздравляем!!!\n${it.asUser(sqlData.guild).lowDescriptor()} cделал Пентакилл$textPentas за ${LeagueMainObject.findHeroForKey(part.championId.toString())} убив: ${arrayHeroName.filter { it.teamId != part.teamId }.joinToString { LeagueMainObject.findHeroForKey(it.championId.toString()) }}\nМатч: ${match.metadata.matchId} Дата: ${match.info.gameCreation.toFormatDateTime()}")
                            }
                        }
                    }
                }
            }

            //Создаем нового игрока в БД
            if (curLOL == null) {
                curLOL = TableLOLPerson(
                    LOL_puuid = part.puuid,
                    LOL_summonerId = part.summonerId,
                    LOL_summonerName = part.summonerName,
                    LOL_riotIdName = part.riotIdName,
                    LOL_riotIdTagline = part.riotIdTagline).save()
            }

            //Вдруг что изменится в профиле игрока
            if (curLOL != null) {
                if (curLOL.LOL_summonerName != part.summonerName || curLOL.LOL_riotIdTagline != part.riotIdTagline || curLOL.LOL_summonerId != part.summonerId) {
                    val textData = "[addMatch::update] LOL_summonerName: old ${curLOL.LOL_summonerName} new ${part.summonerName}" +
                            " LOL_riotIdTagline: old ${curLOL.LOL_riotIdTagline} new ${part.riotIdTagline}" +
                            " LOL_summonerId: old ${curLOL.LOL_summonerId} new ${part.summonerId}"
                    printLog(sqlData.guild, textData)

                    //если чтото меняется у сохраненных пользователей - отсылаем Email
                    if (kordLol != null && kordLol.find { it.LOLperson?.id == curLOL.id } != null) {
                        sqlData.guildSQL.sendEmail(textData)
                    }

                    curLOL.update(TableLOLPerson::LOL_summonerName, TableLOLPerson::LOL_riotIdTagline, TableLOLPerson::LOL_summonerId){
                        LOL_summonerName = part.summonerName
                        LOL_summonerId = part.summonerId
                        LOL_riotIdTagline = part.riotIdTagline
                    }
                }
            }

            TableParticipant(part, pMatch, curLOL!!).save()
        }
        if (kordLol != null && tableMMR != null) {
            calculateMMR(sqlData, pMatch, isSurrender, isBots, kordLol, tableMMR)
        }

        return pMatch
    }

    private fun calculateMMR(sqlData: SQLData, pMatch: TableMatch, isSurrender: Boolean, isBots: Boolean, kordLol: List<TableKORD_LOL>, tableMMR: List<TableMmr>) {
        try {
            asyncLaunch {
                val myParts = tableParticipant.selectAll().where { TableParticipant::match eq pMatch.id }.where { TableParticipant::LOLperson.inList(kordLol.map { it.LOLperson?.id }) }.getEntities()
                val users = myParts.joinToString { (it.LOLperson?.LOL_summonerName?:"") + " hero: ${it.championName} ${CalculateMMR(sqlData, it, pMatch, kordLol, tableMMR.find { mmr -> mmr.champion == it.championName })}\n" }
                sqlData.guild.sendMessage(messageIdDebug,
                    "Добавлен матч: ${pMatch.matchId} ID: ${pMatch.id}\n" +
                            "${pMatch.matchDate.toFormatDateTime()} - ${pMatch.matchDateEnd.toFormatDateTime()}\n" +
                            "Mode: ${pMatch.matchMode} Surrender: $isSurrender Bots: $isBots\n" +
                            "Users: $users")
            }
        }catch (e: Exception) {
            printLog(sqlData.guild, "[calculateMMR] error: ${e.localizedMessage}")
        }
    }

    fun sendEmail(message: String) {
        try {
            GMailSender("llps.sys.bot@gmail.com", "esjk bphc hsjh otcx")
            .sendMail(
                "LOLPentaStealBot - $name",
                message,
                "llps.sys.bot@gmail.com",
                "kaltemeis@gmail.com"
            )
        }catch (e: Exception) {
            e.printStackTrace()
        }
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