package ru.descend.bot.postgre.calculating

import ru.descend.bot.BONUS_MMR_FOR_LVP_ARAM
import ru.descend.bot.BONUS_MMR_FOR_MVP_ARAM
import ru.descend.bot.LOAD_MMR_HEROES_MATCHES
import ru.descend.bot.asyncLaunch
import ru.descend.bot.lolapi.LeagueMainObject
import ru.descend.bot.lolapi.dto.match_dto.MatchDTO
import ru.descend.bot.lolapi.dto.match_dto.Participant
import ru.descend.bot.postgre.SQLData_R2DBC
import ru.descend.bot.postgre.R2DBC
import ru.descend.bot.postgre.r2dbc.model.LOLs
import ru.descend.bot.postgre.r2dbc.model.Matches
import ru.descend.bot.postgre.r2dbc.model.Participants
import ru.descend.bot.datas.create
import ru.descend.bot.postgre.r2dbc.model.LOLs.Companion.tbl_lols
import ru.descend.bot.postgre.r2dbc.model.Participants.Companion.tbl_participants
import ru.descend.bot.datas.update
import ru.descend.bot.generateAIText
import ru.descend.bot.launch
import ru.descend.bot.lowDescriptor
import ru.descend.bot.printLog
import ru.descend.bot.sendMessage
import ru.descend.bot.to1Digits
import ru.descend.bot.toFormatDate
import ru.descend.bot.toFormatDateTime
import kotlin.time.DurationUnit
import kotlin.time.toDuration

data class Calc_AddMatch (
    val sqlData: SQLData_R2DBC,
    val match: MatchDTO
) {

    val arrayOtherLOLs = ArrayList<LOLs>()

    suspend fun calculate(mainOrder: Boolean) : Matches {
        arrayOtherLOLs.clear()
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

        val pMatchResult = Matches(
            matchId = match.metadata.matchId,
            matchDateStart = match.info.gameStartTimestamp,
            matchDateEnd = match.info.gameEndTimestamp,
            matchDuration = match.info.gameDuration,
            matchMode = match.info.gameMode,
            matchGameVersion = match.info.gameVersion,
            bots = isBots,
            region = match.metadata.matchId.substringBefore("_"),
            surrender = isSurrender
        ).create(Matches::matchId, "[atom:${sqlData.atomicIntLoaded.get()}]")

        if (!pMatchResult.bit) return pMatchResult.result
        val pMatch = pMatchResult.result

        if (pMatch.id % LOAD_MMR_HEROES_MATCHES == 0){
            R2DBC.executeProcedure("call \"GetAVGs\"()")
            sqlData.dataMMR.clear()
            LeagueMainObject.catchHeroNames()
        }

        val arrayHeroName = ArrayList<Participant>()
        match.info.participants.forEach {part ->
            arrayHeroName.add(part)
        }

        val savedLOL = sqlData.dataSavedLOL.get()
        val curLOLs = R2DBC.getLOLs { tbl_lols.LOL_puuid.inList(arrayHeroName.map { it.puuid }) }
        val arrayNewParts = ArrayList<Participants>()
        val lastLolsList = ArrayList<LOLs>()
        match.info.participants.forEach {part ->
            var curLOL = curLOLs.find { it.LOL_puuid == part.puuid }

            //Создаем нового игрока в БД
            if (curLOL == null || curLOL.id == 0) {
                curLOL = LOLs(LOL_puuid = part.puuid,
                    LOL_summonerId = part.summonerId,
                    LOL_riotIdName = if (part.riotIdGameName == "null") part.summonerName else part.riotIdGameName,
                    LOL_riotIdTagline = part.riotIdTagline,
                    LOL_summonerLevel = part.summonerLevel,
                    LOL_region = pMatch.getRegionValue(),
                    profile_icon = part.profileIcon).create(LOLs::LOL_puuid).result
            } else if (!curLOL.isBot()) {
                //Вдруг что изменится в профиле игрока
                if (curLOL.LOL_summonerLevel < part.summonerLevel || (curLOL.LOL_summonerLevel == part.summonerLevel && (curLOL.LOL_region != pMatch.getRegionValue() || curLOL.LOL_riotIdTagline != part.riotIdTagline || curLOL.LOL_summonerId != part.summonerId || curLOL.LOL_riotIdName != part.riotIdGameName || curLOL.profile_icon != part.profileIcon))) {
                    curLOL.LOL_riotIdTagline = part.riotIdTagline
                    curLOL.LOL_region = pMatch.getRegionValue()
                    curLOL.LOL_summonerId = part.summonerId
                    val newName = if (part.riotIdGameName == "null") part.summonerName else part.riotIdGameName
                    if (newName != "null") curLOL.LOL_riotIdName = newName
                    curLOL.LOL_summonerLevel = part.summonerLevel
                    curLOL.profile_icon = part.profileIcon
                    curLOL = curLOL.update()
                }
            }

            asyncLaunch {
                val savedKORDLOL = sqlData.getSavedLOL(curLOL)
                //Проверка пентакилла
                if (savedKORDLOL != null && part.pentaKills > 0) {
                    val championName = R2DBC.getHeroFromKey(part.championId.toString())?.nameRU?:""
                    val textPentasCount = if (part.pentaKills == 1) "" else "(${part.pentaKills})"
                    val generatedText = generateAIText("Напиши необычное и странное поздравление пользователю ${savedKORDLOL.asUser(sqlData.guild, sqlData).lowDescriptor()} за то что он сделал Пентакилл в игре League of Legends за чемпиона $championName")
                    val resultText = "Поздравляем!!!\n${savedKORDLOL.asUser(sqlData.guild, sqlData).lowDescriptor()} cделал Пентакилл$textPentasCount за $championName\nМатч: ${match.metadata.matchId} Дата: ${match.info.gameCreation.toFormatDateTime()}\n\n$generatedText"
                    sqlData.sendMessage(sqlData.guildSQL.messageIdStatus, resultText)
                }
            }

            lastLolsList.add(curLOL)

            if (!curLOL.isBot() && sqlData.dataKORDLOL.get().find { tbl -> tbl.LOL_id == curLOL.id } == null)
                arrayOtherLOLs.add(curLOL)

            arrayNewParts.add(Participants(part, pMatch, curLOL))
        }

        arrayOtherLOLs.removeIf { savedLOL.find { finded -> finded.id == it.id } != null }
        val lastPartList = R2DBC.addBatchParticipants(arrayNewParts)

        calculateMMR(pMatch, mainOrder, lastPartList, lastLolsList)
        if (!mainOrder) {
            sqlData.textNewMatches.appendLine("${pMatch.matchId} ${pMatch.id} ${pMatch.matchMode} ${pMatch.matchDateEnd.toFormatDate()}\n", pMatch.id.toString())
        }

        return pMatch
    }

    private suspend fun calculateMMR(
        pMatch: Matches,
        mainOrder: Boolean,
        lastPartList: List<Participants>,
        lastLolsList: List<LOLs>,
    ) {

        if (pMatch.matchMode != "ARAM") return

        var users = ""

        val arrayKORDmmr = ArrayList<Pair<LOLs, Participants>>()
        lastPartList.forEach { par ->
            val lolObj = lastLolsList.find { it.id == par.LOLperson_id }!!
            val data = Calc_MMR(par, pMatch, lolObj, sqlData.getMMRforChampion(par.championName))
            val copyLol = lolObj.copy()
            data.init()
            arrayKORDmmr.add(Pair(data.getSavedLOL(), data.getSavedParticipant()))
            if (mainOrder) {
                users += "* __" + sqlData.getLOL(par.LOLperson_id)?.getCorrectName() + "__ ${R2DBC.getHeroFromKey(par.championId.toString())?.nameRU?:"null"} win:${par.win} count:${R2DBC.getParticipantsSize { tbl_participants.LOLperson_id eq par.LOLperson_id }} $data\n" +
                        "OLDLOL: $copyLol\n" +
                        "NEWLOL: $lolObj\n"
            }
        }

        if (arrayKORDmmr.isNotEmpty()) {
            //Обработка MVP LVP
            arrayKORDmmr.sortBy { it.first.mmrAramLast }
            arrayKORDmmr.first().let { first ->
                first.first.mmrAramLastText = "LVP"
                first.first.mmrAramLast = (first.first.mmrAramLast - BONUS_MMR_FOR_LVP_ARAM).to1Digits()
                first.first.removeMMRvalue(BONUS_MMR_FOR_LVP_ARAM.to1Digits())
            }
            arrayKORDmmr.last().let { last ->
                last.first.mmrAramLastText = "MVP"
                last.first.mmrAramLast = (last.first.mmrAramLast + BONUS_MMR_FOR_MVP_ARAM).to1Digits()
                last.first.mmrAram = (last.first.mmrAram + BONUS_MMR_FOR_MVP_ARAM).to1Digits()

            }
            //Перезапись полей для сохранения в базу
            arrayKORDmmr.forEach {
                it.first.update()
            }
        }

        if (mainOrder) {
            var minsDuration: Int
            var secondsDuration: Int
            pMatch.matchDuration.toDuration(DurationUnit.SECONDS).toComponents { _, minutes, seconds, _ ->
                minsDuration = minutes
                secondsDuration = seconds
            }
            sqlData.sendMessage(sqlData.guildSQL.messageIdDebug,
                "**Добавлен матч: ${pMatch.matchId} ID: ${pMatch.id}\n" +
                        "${pMatch.matchDateStart.toFormatDateTime()} - ${pMatch.matchDateEnd.toFormatDateTime()}\n" +
                        "Duration: $minsDuration:$secondsDuration\n" +
                        "Mode: ${pMatch.matchMode} Surrender: ${pMatch.surrender} Bots: ${pMatch.bots}**\n$users"
            )
        }
    }
}