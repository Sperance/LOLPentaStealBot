package ru.descend.bot.postgre.calculating

import ru.descend.bot.asyncLaunch
import ru.descend.bot.generateAIText
import ru.descend.bot.lolapi.LeagueMainObject
import ru.descend.bot.lolapi.dto.match_dto.MatchDTO
import ru.descend.bot.lolapi.dto.match_dto.Participant
import ru.descend.bot.lowDescriptor
import ru.descend.bot.postgre.SQLData_R2DBC
import ru.descend.bot.postgre.R2DBC
import ru.descend.bot.postgre.r2dbc.model.KORDLOLs
import ru.descend.bot.postgre.r2dbc.model.LOLs
import ru.descend.bot.postgre.r2dbc.model.Matches
import ru.descend.bot.postgre.r2dbc.model.Participants
import ru.descend.bot.datas.create
import ru.descend.bot.postgre.r2dbc.model.LOLs.Companion.tbl_lols
import ru.descend.bot.postgre.r2dbc.model.Matches.Companion.tbl_matches
import ru.descend.bot.postgre.r2dbc.model.Participants.Companion.tbl_participants
import ru.descend.bot.datas.update
import ru.descend.bot.printLog
import ru.descend.bot.datas.isCurrentDay
import ru.descend.bot.sendMessage
import ru.descend.bot.to1Digits
import ru.descend.bot.toDate
import ru.descend.bot.toFormatDate
import ru.descend.bot.toFormatDateTime
import ru.descend.bot.writeLog
import kotlin.time.DurationUnit
import kotlin.time.toDuration

data class Calc_AddMatch (
    val sqlData: SQLData_R2DBC,
    val match: MatchDTO,
    val kordLol: ArrayList<KORDLOLs>
) {

    val arrayOtherLOLs = ArrayList<LOLs>()

    suspend fun calculate(mainOrder: Boolean) : Matches {
        arrayOtherLOLs.clear()
        var isBots = false
        var isSurrender = false

//        val alreadyMatch = R2DBC.getMatchOne({tbl_matches.matchId eq match.metadata.matchId})
//        if (alreadyMatch != null) {
//            if (mainOrder) printLog("Match ${alreadyMatch.id} ${alreadyMatch.matchId} already exists")
//            return alreadyMatch
//        }

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
            bots = isBots,
            region = match.metadata.matchId.substringBefore("_"),
            surrender = isSurrender
        ).create(Matches::matchId)

        if (pMatch.id % 5000 == 0){
            R2DBC.executeProcedure("call \"GetAVGs\"()")
            LeagueMainObject.catchHeroNames()
        }

        val arrayHeroName = ArrayList<Participant>()
        match.info.participants.forEach {part ->
            arrayHeroName.add(part)
        }

        val savedLOL = sqlData.dataSavedLOL.get()
        val curLOLs = R2DBC.getLOLs { tbl_lols.LOL_puuid.inList(arrayHeroName.map { it.puuid }) }
        var isNeedCalcMMR = false
        val arrayNewParts = ArrayList<Participants>()
        match.info.participants.forEach {part ->
            var curLOL = curLOLs.find { it.LOL_puuid == part.puuid }
            if (!isNeedCalcMMR && savedLOL.find { lol -> lol.LOL_puuid == part.puuid } != null) {
                isNeedCalcMMR = true
            }

            if (curLOL != null && !isBots && !isSurrender){
                kordLol.find { it.LOL_id == curLOL?.id }?.let {
                    asyncLaunch {
                        if (part.pentaKills > 0 && (match.info.gameCreation.toDate().isCurrentDay() || match.info.gameEndTimestamp.toDate().isCurrentDay())) {
                            val championName = LeagueMainObject.findHeroForKey(part.championId.toString())
                            val textPentasCount = if (part.pentaKills == 1) "" else "(${part.pentaKills})"
                            val generatedText = generateAIText("Напиши необычное и оригинальное и длинное поздравление пользователю ${it.asUser(sqlData.guild, sqlData).lowDescriptor()} за то что он сделал Пентакилл в игре League of Legends за чемпиона $championName от имени Discord сервера АрамоЛолево")
                            val resultText = "Поздравляем!!!\n${it.asUser(sqlData.guild, sqlData).lowDescriptor()} cделал Пентакилл$textPentasCount за $championName убив: ${arrayHeroName.filter { it.teamId != part.teamId }.joinToString { LeagueMainObject.findHeroForKey(it.championId.toString()) }}\nМатч: ${match.metadata.matchId} Дата: ${match.info.gameCreation.toFormatDateTime()}\n\n$generatedText"

                            if (pMatch.matchMode == "CLASSIC") {
                                it.mmrAramSaved += 5.0
                                it.update()
                            }

                            sqlData.sendMessage(sqlData.guildSQL.messageIdStatus, resultText)
                            writeLog(resultText)
                        }
                    }
                }
            }

            //Создаем нового игрока в БД
            if (curLOL == null || curLOL.id == 0) {
                curLOL = LOLs(LOL_puuid = part.puuid,
                    LOL_summonerId = part.summonerId,
                    LOL_riotIdName = if (part.riotIdGameName == "null") part.summonerName else part.riotIdGameName,
                    LOL_riotIdTagline = part.riotIdTagline,
                    LOL_summonerLevel = part.summonerLevel,
                    LOL_region = pMatch.getRegionValue(),
                    profile_icon = part.profileIcon).create(LOLs::LOL_puuid)
            } else if (!curLOL.isBot()) {
                //Вдруг что изменится в профиле игрока
                if (curLOL.LOL_summonerLevel < part.summonerLevel || (curLOL.LOL_summonerLevel == part.summonerLevel && (curLOL.LOL_region != pMatch.getRegionValue() || curLOL.LOL_riotIdTagline != part.riotIdTagline || curLOL.LOL_summonerId != part.summonerId || curLOL.LOL_riotIdName != part.riotIdGameName || curLOL.profile_icon != part.profileIcon))) {
                    curLOL.LOL_riotIdTagline = part.riotIdTagline
                    curLOL.LOL_region = pMatch.getRegionValue()
                    curLOL.LOL_summonerId = part.summonerId
                    curLOL.LOL_riotIdName = if (part.riotIdGameName == "null") part.summonerName else part.riotIdGameName
                    curLOL.LOL_summonerLevel = part.summonerLevel
                    curLOL.profile_icon = part.profileIcon
                    curLOL = curLOL.update()
                }
            }

            if (!curLOL.isBot() && sqlData.dataKORDLOL.get().find { tbl -> tbl.LOL_id == curLOL.id } == null)
                arrayOtherLOLs.add(curLOL)

            arrayNewParts.add(Participants(part, pMatch, curLOL))
        }

        arrayOtherLOLs.removeIf { savedLOL.find { finded -> finded.id == it.id } != null }
        R2DBC.addBatchParticipants(arrayNewParts)

        if (isNeedCalcMMR) {
            sqlData.isNeedUpdateDatas = true
            calculateMMR(pMatch, isSurrender, isBots, kordLol)
        }
        if (!mainOrder) {
            sqlData.textNewMatches.appendLine("${pMatch.matchId} ${pMatch.id} ${pMatch.matchMode} ${pMatch.matchDateEnd.toFormatDate()}\n", pMatch.id.toString(), pMatch.matchId)
        }

        return pMatch
    }

    private suspend fun calculateMMR(pMatch: Matches, isSurrender: Boolean, isBots: Boolean, kordLol: List<KORDLOLs>) {
        var users = ""
        val arrayKORDmmr = ArrayList<Triple<KORDLOLs?, Participants, Double>>()
        pMatch.getParticipants().forEach { par ->
            val dataText = if (pMatch.matchMode == "ARAM") {
                val data = Calc_MMR(sqlData, par, pMatch, kordLol, sqlData.getMMRforChampion(par.championName))
                data.init()
                arrayKORDmmr.add(Triple(kordLol.find { kd -> kd.LOL_id == par.LOLperson_id }, par, data.mmrValue))
                data.toString()
            } else {
                ""
            }
            users += "* __" + sqlData.getLOL(par.LOLperson_id)?.getCorrectName() + "__ ${LeagueMainObject.findHeroForKey(par.championId.toString())} win:${par.win} $dataText\n"
        }

        //Обработка MVP
        if (arrayKORDmmr.isNotEmpty()){
            val mmrForMVP = 2.0
            val maxed = arrayKORDmmr.maxBy { it.third }
            if (maxed.first != null) {
                maxed.second.mvpLvpInfo = "MVP"
                maxed.second.mmr = (maxed.second.mmr + mmrForMVP).to1Digits()
                maxed.second.update()

                maxed.first!!.mmrAram = (maxed.first!!.mmrAram + mmrForMVP).to1Digits()
                maxed.first!!.update()
            }
        }

        var minsDuration: Int
        var secondsDuration: Int
        pMatch.matchDuration.toDuration(DurationUnit.SECONDS).toComponents { hours, minutes, seconds, nanoseconds ->
            minsDuration = minutes
            secondsDuration = seconds
        }
        sqlData.sendMessage(sqlData.guildSQL.messageIdDebug,
            "**Добавлен матч: ${pMatch.matchId} ID: ${pMatch.id}\n" +
                    "${pMatch.matchDateStart.toFormatDateTime()} - ${pMatch.matchDateEnd.toFormatDateTime()}\n" +
                    "Duration: $minsDuration:$secondsDuration\n" +
                    "Mode: ${pMatch.matchMode} Surrender: $isSurrender Bots: $isBots**\n$users"
        )
    }
}