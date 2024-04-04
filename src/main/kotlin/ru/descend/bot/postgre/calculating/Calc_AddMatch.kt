package ru.descend.bot.postgre.calculating

import ru.descend.bot.asyncLaunch
import ru.descend.bot.lolapi.LeagueMainObject
import ru.descend.bot.lolapi.leaguedata.match_dto.MatchDTO
import ru.descend.bot.lolapi.leaguedata.match_dto.Participant
import ru.descend.bot.lowDescriptor
import ru.descend.bot.postgre.SQLData_R2DBC
import ru.descend.bot.postgre.r2dbc.R2DBC
import ru.descend.bot.postgre.r2dbc.model.KORDLOLs
import ru.descend.bot.postgre.r2dbc.model.LOLs
import ru.descend.bot.postgre.r2dbc.model.Matches
import ru.descend.bot.postgre.r2dbc.model.Participants
import ru.descend.bot.postgre.r2dbc.create
import ru.descend.bot.postgre.r2dbc.model.LOLs.Companion.tbl_lols
import ru.descend.bot.postgre.r2dbc.update
import ru.descend.bot.savedObj.isCurrentDay
import ru.descend.bot.sendMessage
import ru.descend.bot.toDate
import ru.descend.bot.toFormatDateTime

data class Calc_AddMatch (
    val sqlData: SQLData_R2DBC,
    val match: MatchDTO,
    val kordLol: ArrayList<KORDLOLs>? = null
) {
    suspend fun calculate() : Matches {
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

        val pMatch = Matches(
            matchId = match.metadata.matchId,
            matchDateStart = match.info.gameStartTimestamp,
            matchDateEnd = match.info.gameEndTimestamp,
            matchDuration = match.info.gameDuration,
            matchMode = match.info.gameMode,
            matchGameVersion = match.info.gameVersion,
            guild_id = sqlData.guildSQL.id,
            bots = isBots,
            surrender = isSurrender
        ).create(Matches::matchId)
        sqlData.isNeedUpdateDatas = true

//        if (pMatch.id % 1000 == 0){
//            asyncLaunch {
//                sqlData.sendEmail("Sys", "execute method AVGs()")
//                sqlData.executeProcedure("call \"GetAVGs\"()")
//            }
//        }

        val arrayHeroName = ArrayList<Participant>()
        match.info.participants.forEach {part ->
            arrayHeroName.add(part)
        }

        val arrayNewParts = ArrayList<Participants>()
        val lolsArray = R2DBC.getLOLs { tbl_lols.LOL_puuid.inList(arrayHeroName.map { it.puuid }) }
        match.info.participants.forEach {part ->
            var curLOL = lolsArray.find { it.LOL_puuid == part.puuid }

            if (kordLol != null && curLOL != null && !isBots && !isSurrender){
                kordLol.find { it.LOL_id == curLOL?.id }?.let {
                    asyncLaunch {
                        if (part.pentaKills > 0 && (match.info.gameCreation.toDate().isCurrentDay() || match.info.gameEndTimestamp.toDate().isCurrentDay())) {
                            val textPentas = if (part.pentaKills == 1) "" else "(${part.pentaKills})"
                            sqlData.sendMessage(sqlData.guildSQL.messageIdStatus, "Поздравляем!!!\n${it.asUser(sqlData.guild, sqlData).lowDescriptor()} cделал Пентакилл$textPentas за ${LeagueMainObject.findHeroForKey(part.championId.toString())} убив: ${arrayHeroName.filter { it.teamId != part.teamId }.joinToString { LeagueMainObject.findHeroForKey(it.championId.toString()) }}\nМатч: ${match.metadata.matchId} Дата: ${match.info.gameCreation.toFormatDateTime()}")
                        }
                    }
                }
            }

            //Создаем нового игрока в БД
            var isNewLOL = false
            if (curLOL == null) {
                curLOL = LOLs(LOL_puuid = part.puuid,
                    LOL_summonerId = part.summonerId,
                    LOL_summonerName = part.summonerName,
                    LOL_riotIdName = part.riotIdGameName,
                    LOL_riotIdTagline = part.riotIdTagline)
                isNewLOL = true
            }

            //Вдруг что изменится в профиле игрока
            if (curLOL.LOL_summonerLevel != part.summonerLevel || curLOL.LOL_summonerName != part.summonerName || curLOL.LOL_riotIdTagline != part.riotIdTagline || curLOL.LOL_summonerId != part.summonerId || curLOL.LOL_riotIdName != part.riotIdGameName) {
                curLOL.LOL_summonerName = part.summonerName
                curLOL.LOL_riotIdTagline = part.riotIdTagline
                curLOL.LOL_summonerId = part.summonerId
                curLOL.LOL_riotIdName = part.riotIdGameName
                curLOL.LOL_summonerLevel = part.summonerLevel
                curLOL = if (isNewLOL) curLOL.create(LOLs::LOL_puuid)
                else curLOL.update()
            }

            arrayNewParts.add(Participants(part, pMatch, curLOL))
        }
        R2DBC.addBatchParticipants(arrayNewParts)

        if (kordLol != null) {
            calculateMMR(pMatch, isSurrender, isBots, kordLol)
        }

        return pMatch
    }

    private suspend fun calculateMMR(pMatch: Matches, isSurrender: Boolean, isBots: Boolean, kordLol: List<KORDLOLs>) {
        var users = ""
        sqlData.getSavedParticipantsForMatch(pMatch.id).forEach {
            val data = Calc_MMR(sqlData, it, pMatch, kordLol, sqlData.getMMRforChampion(it.championName))
            data.init()
            users += sqlData.getLOL(it.LOLperson_id)?.LOL_summonerName + " hero: ${it.championName} $data\n"
        }
        sqlData.sendMessage(sqlData.guildSQL.messageIdDebug,
            "Добавлен матч: ${pMatch.matchId} ID: ${pMatch.id}\n" +
                    "${pMatch.matchDateStart.toFormatDateTime()} - ${pMatch.matchDateEnd.toFormatDateTime()}\n" +
                    "Mode: ${pMatch.matchMode} Surrender: $isSurrender Bots: $isBots\n" +
                    "Users: $users")
    }
}