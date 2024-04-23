package ru.descend.bot.postgre.calculating

import ru.descend.bot.asyncLaunch
import ru.descend.bot.lolapi.LeagueMainObject
import ru.descend.bot.lolapi.dto.match_dto.MatchDTO
import ru.descend.bot.lolapi.dto.match_dto.Participant
import ru.descend.bot.lowDescriptor
import ru.descend.bot.postgre.SQLData_R2DBC
import ru.descend.bot.postgre.r2dbc.R2DBC
import ru.descend.bot.postgre.r2dbc.model.KORDLOLs
import ru.descend.bot.postgre.r2dbc.model.LOLs
import ru.descend.bot.postgre.r2dbc.model.Matches
import ru.descend.bot.postgre.r2dbc.model.Participants
import ru.descend.bot.postgre.r2dbc.create
import ru.descend.bot.postgre.r2dbc.model.LOLs.Companion.tbl_lols
import ru.descend.bot.postgre.r2dbc.model.Participants.Companion.tbl_participants
import ru.descend.bot.postgre.r2dbc.update
import ru.descend.bot.savedObj.Gemini
import ru.descend.bot.savedObj.isCurrentDay
import ru.descend.bot.sendMessage
import ru.descend.bot.toDate
import ru.descend.bot.toFormatDateTime
import ru.descend.bot.writeLog

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
                            val championName = LeagueMainObject.findHeroForKey(part.championId.toString())
                            val textPentasCount = if (part.pentaKills == 1) "" else "(${part.pentaKills})"
                            val textPenta = Gemini.generateForText("Напиши поздравление для игрока ${curLOL!!.getCorrectName()} который сделал Пентакилл в игре League of Legends за чемпиона $championName")
                            val resultText = "Поздравляем!!!\n${it.asUser(sqlData.guild, sqlData).lowDescriptor()} cделал Пентакилл$textPentasCount за $championName убив: ${arrayHeroName.filter { it.teamId != part.teamId }.joinToString { LeagueMainObject.findHeroForKey(it.championId.toString()) }}\nМатч: ${match.metadata.matchId} Дата: ${match.info.gameCreation.toFormatDateTime()}\n\n$textPenta"
                            sqlData.sendMessage(sqlData.guildSQL.messageIdStatus, resultText)
                            writeLog(resultText)
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
            if (curLOL.LOL_summonerLevel != part.summonerLevel || curLOL.LOL_summonerName != part.summonerName || curLOL.LOL_riotIdTagline != part.riotIdTagline || curLOL.LOL_summonerId != part.summonerId || curLOL.LOL_riotIdName != part.riotIdGameName || curLOL.profile_icon != part.profileIcon) {
                curLOL.LOL_summonerName = part.summonerName
                curLOL.LOL_riotIdTagline = part.riotIdTagline
                curLOL.LOL_summonerId = part.summonerId
                curLOL.LOL_riotIdName = part.riotIdGameName
                curLOL.LOL_summonerLevel = part.summonerLevel
                curLOL.profile_icon = part.profileIcon
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
        val arrayKORDmmr = ArrayList<Triple<KORDLOLs?, Participants, Double>>()
        R2DBC.getParticipants { tbl_participants.match_id eq pMatch.id ; tbl_participants.guild_id eq sqlData.guildSQL.id }.forEach {par ->
            val dataText = if (pMatch.matchMode == "ARAM") {
                val data = Calc_MMR(sqlData, par, pMatch, kordLol, sqlData.getMMRforChampion(par.championName))
                data.init()
                arrayKORDmmr.add(Triple(kordLol.find { kd -> kd.LOL_id == par.LOLperson_id }, par, data.mmrValue))
                data.toString()
            } else {
                ""
            }
            val lolObj = sqlData.getLOL(par.LOLperson_id)
            users += "* __" + lolObj?.getCorrectName() + "__ ${LeagueMainObject.findHeroForKey(par.championId.toString())} size: ${R2DBC.getParticipants { tbl_participants.LOLperson_id eq par.LOLperson_id }.size} win:${par.win} $dataText\n"
        }

        //Обработка MVP
        if (arrayKORDmmr.isNotEmpty()){
            val mmrForMVP = 2.0
            val maxed = arrayKORDmmr.maxBy { it.third }
            if (maxed.first != null) {
                maxed.second.mvpLvpInfo = "MVP"
                maxed.second.mmr += mmrForMVP
                maxed.second.update()

                maxed.first!!.mmrAram += mmrForMVP
                maxed.first!!.update()
            }
        }

        sqlData.sendMessage(sqlData.guildSQL.messageIdDebug,
            "**Добавлен матч: ${pMatch.matchId} ID: ${pMatch.id}\n" +
                    "${pMatch.matchDateStart.toFormatDateTime()} - ${pMatch.matchDateEnd.toFormatDateTime()}\n" +
                    "Mode: ${pMatch.matchMode} Surrender: $isSurrender Bots: $isBots**\n$users"
        )
    }
}