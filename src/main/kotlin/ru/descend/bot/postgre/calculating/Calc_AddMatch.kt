package ru.descend.bot.postgre.calculating

import org.komapper.core.dsl.QueryDsl
import ru.descend.bot.LOAD_MMR_HEROES_MATCHES
import ru.descend.bot.LVP_TAG
import ru.descend.bot.MVP_TAG
import ru.descend.bot.asyncLaunch
import ru.descend.bot.atomicIntLoaded
import ru.descend.bot.lolapi.LeagueMainObject
import ru.descend.bot.postgre.SQLData_R2DBC
import ru.descend.bot.postgre.R2DBC
import ru.descend.bot.postgre.r2dbc.model.LOLs
import ru.descend.bot.postgre.r2dbc.model.Matches
import ru.descend.bot.datas.create
import ru.descend.bot.datas.getData
import ru.descend.bot.datas.update
import ru.descend.bot.postgre.r2dbc.model.LOLs.Companion.tbl_lols
import ru.descend.bot.lolapi.dto.matchDto.MatchDTO
import ru.descend.bot.lolapi.dto.matchDto.Participant
import ru.descend.bot.postgre.db
import ru.descend.bot.postgre.r2dbc.model.ParticipantsNew
import ru.descend.bot.postgre.r2dbc.model.ParticipantsNew.Companion.tbl_participantsnew
import ru.descend.bot.printLog
import ru.descend.bot.printLogMMR
import ru.descend.bot.sendMessage
import ru.descend.bot.to1Digits
import ru.descend.bot.toFormatDate
import ru.descend.kotlintelegrambot.handlers.listening_data_array
import kotlin.math.abs

data class Calc_AddMatch (
    val sqlData: SQLData_R2DBC,
    val match: MatchDTO
) {
    suspend fun calculate() : Matches {
        var isBots = false
        var isSurrender = false
        var isAborted = false

        match.info.participants.forEach {
            if (it.summonerId == "BOT" || it.puuid == "BOT") {
                isBots = true
            }
            if (it.gameEndedInEarlySurrender || it.teamEarlySurrendered) {
                isSurrender = true
            }
        }

        //Чёт новое завезли
        if (match.info.endOfGameResult != null && match.info.endOfGameResult != "GameComplete") {
            isAborted = true
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
            surrender = isSurrender,
            endOfGameResult = match.info.endOfGameResult?:"",
            aborted = isAborted,
            mapId = match.info.mapId,
            gameType = match.info.gameType
        ).create(Matches::matchId, "[atom:${atomicIntLoaded.get()}]")

        if (!pMatchResult.bit) return pMatchResult.result
        val pMatch = pMatchResult.result

        if (pMatch.matchMode == "ARAM") sqlData.isHaveLastARAM = true

        if (pMatch.id % LOAD_MMR_HEROES_MATCHES == 0){
            R2DBC.executeProcedure("call \"GetAVGs\"()")
            LeagueMainObject.catchHeroNames()
        }

        val arrayHeroName = ArrayList<Participant>()
        match.info.participants.forEach {part ->
            //Скипуем матчи если хотя бы 1 чел афк
            if ((part.kills == 0 && part.deaths == 0 && part.assists == 0) || (part.itemsPurchased <= 1)) {
                printLog("[ADDMTACH] match ${pMatch.matchId} skipped for AFK or Troll.")
                return pMatch
            }

            arrayHeroName.add(part)
        }

        val curLOLs = LOLs().getData({ tbl_lols.LOL_puuid.inList(arrayHeroName.map { it.puuid }) })
        val arrayNewParts = ArrayList<ParticipantsNew>()
        val lastLolsList = ArrayList<LOLs>()
        match.info.participants.forEach {part ->
            var curLOL = curLOLs.find { it.LOL_puuid == part.puuid }

            //Создаем нового игрока в БД
            if (curLOL == null || curLOL.id == 0) {
                curLOL = LOLs(LOL_puuid = part.puuid,
                    LOL_summonerId = part.summonerId,
                    LOL_riotIdName = part.riotIdGameName,
                    LOL_riotIdTagline = part.riotIdTagline,
                    LOL_summonerLevel = part.summonerLevel,
                    LOL_region = pMatch.getRegionValue(),
                    profile_icon = part.profileIcon).create(LOLs::LOL_puuid).result
            } else if (!curLOL.isBot()) {
                curLOL.LOL_riotIdTagline = part.riotIdTagline
                curLOL.LOL_region = pMatch.getRegionValue()
                curLOL.LOL_summonerId = part.summonerId
                val newName = part.riotIdGameName
                if (newName != "null") curLOL.LOL_riotIdName = newName
                curLOL.LOL_summonerLevel = part.summonerLevel
                curLOL.profile_icon = part.profileIcon
                curLOL = curLOL.update()
            }

            lastLolsList.add(curLOL)
            if (pMatch.isNeedCalcStats() && sqlData.getKORDLOL().find { kl -> kl.LOL_id == curLOL.id } != null)
                arrayNewParts.add(ParticipantsNew(part, pMatch, curLOL))
        }

        if (pMatch.isNeedCalcStats()) {
            val lastPartList = db.runQuery { QueryDsl.insert(tbl_participantsnew).multiple(arrayNewParts) }
            calculateMMR(pMatch, lastPartList, lastLolsList)
        }

        return pMatch
    }

    private suspend fun calculateMMR(pMatch: Matches, lastPartList: List<ParticipantsNew>, lastLolsList: List<LOLs>) {
        val calcv3 = Calc_MMRv3(pMatch)
        val arrayKORDmmr = ArrayList<Pair<LOLs, ParticipantsNew>>()
        lastLolsList.forEach {
            val finededPart = lastPartList.find { par -> par.LOLperson_id == it.id }
            if (finededPart != null) {
                arrayKORDmmr.add(Pair(it, finededPart))
            }
        }
        calcMMR20(pMatch, arrayKORDmmr)
        calcv3.calculateTOPstats(arrayKORDmmr.map { it.second })
        var strToTelegram = ""
        arrayKORDmmr.forEach {
            val veResult = calcv3.calculateNewMMR(it.second, 0.0, listOf(0.0, 0.0, 0.0, 0.0), listOf(0.0, 0.0, 0.0, 0.0, 0.0), it.second.win)
            it.first.update()
            it.second.update()
            strToTelegram += veResult.toStringLow()
            printLogMMR("Результат: $veResult")
        }
        listening_data_array.add(strToTelegram)
        printLog("DATA SIZE: ${listening_data_array.size} Last len: ${listening_data_array.last().length}")
    }

    private fun calcMMR20(pMatch: Matches, data: ArrayList<Pair<LOLs, ParticipantsNew>>) {

        data.forEach {
            it.second.tempTextMMR2 += ";tD:${it.second.teamDamagePercentage * 10};"
            it.second.tempTextMMR2value += it.second.teamDamagePercentage * 10.0

            it.second.tempTextMMR2 += ";dT:${it.second.damageTakenOnTeamPercentage * 10};"
            it.second.tempTextMMR2value += it.second.damageTakenOnTeamPercentage * 10.0

            it.second.tempTextMMR2 += it.second.saveAllyFromDeath * 2.0
        }

        data.sortBy { it.second.kills }
        data.forEachIndexed { index, pair ->
            pair.second.tempTextMMR2 += ";kill:${((index + 1) / 2.0).to1Digits()};"
            pair.second.tempTextMMR2value += ((index + 1) / 2.0).to1Digits()
        }

        data.sortBy { it.second.assists }
        data.forEachIndexed { index, pair ->
            pair.second.tempTextMMR2 += ";assist:${((index + 1) / 2.0).to1Digits()};"
            pair.second.tempTextMMR2value += ((index + 1) / 2.0).to1Digits()
        }

        data.sortByDescending { it.second.deathsByEnemyChamps }
        data.forEachIndexed { index, pair ->
            pair.second.tempTextMMR2 += ";death:${((index + 1) / 2.0).to1Digits()};"
            pair.second.tempTextMMR2value += ((index + 1) / 2.0).to1Digits()
        }

        data.sortBy { it.second.damageSelfMitigated }
        data.forEachIndexed { index, pair ->
            pair.second.tempTextMMR2 += ";dSM:${((index + 1) / 2.0).to1Digits()};"
            pair.second.tempTextMMR2value += ((index + 1) / 2.0).to1Digits()
        }

        data.sortBy { it.second.skillshotsDodged }
        data.forEachIndexed { index, pair ->
            pair.second.tempTextMMR2 += ";ssD:${((index + 1) / 2.0).to1Digits()};"
            pair.second.tempTextMMR2value += ((index + 1) / 2.0).to1Digits()
        }

        data.sortBy { it.second.longestTimeSpentLiving }
        data.forEachIndexed { index, pair ->
            pair.second.tempTextMMR2 += ";lTSL:${((index + 1) / 2.0).to1Digits()};"
            pair.second.tempTextMMR2value += ((index + 1) / 2.0).to1Digits()
        }

        data.forEach {
            it.second.tempTextMMR2value = (it.second.tempTextMMR2value / 2.0).to1Digits()
            it.second.tempTextMMR2 += ";AFTER_R:${it.second.tempTextMMR2value}"
        }

        data.maxBy { it.second.tempTextMMR2value }.second.apply {
            tempTextMMR2value += 5.0
            tempTextMMR2 += ";MVP"
            gameMatchKey = MVP_TAG
        }

        data.minBy { it.second.tempTextMMR2value }.second.apply {
            tempTextMMR2value -= 5.0
            tempTextMMR2 += ";LVP"
            gameMatchKey = LVP_TAG
        }

        data.filter { it.second.win }.forEach {
            it.second.tempTextMMR2 += ";ПОБЕДА"
            it.second.tempTextMMR2value += 5
        }

        val maxMMR = data.maxBy { it.second.tempTextMMR2value }.second.tempTextMMR2value

        data.filter { !it.second.win }.forEach {
            it.second.tempTextMMR2 += ";bef_l:${it.second.tempTextMMR2value}"
            it.second.tempTextMMR2value = (maxMMR - it.second.tempTextMMR2value).to1Digits()
            it.second.tempTextMMR2value = -it.second.tempTextMMR2value
            it.second.tempTextMMR2 += ";aft_l:${it.second.tempTextMMR2value}"
        }

        //Бонусные ММР
        data.forEach {
            var additionalMMR = 0.0
            sqlData.calculatePentakill(it.first, it.second, pMatch)
            additionalMMR += it.second.kills5 * 10.0
            additionalMMR += it.second.kills4 * 5.0
//            additionalMMR += it.second.tookLargeDamageSurvived * 3.0
//            if (it.second.win) additionalMMR += 2.0
            if (additionalMMR > 20.0) additionalMMR = 20.0

            it.first.mmrAramSaved = (it.first.mmrAramSaved + additionalMMR).to1Digits()

            it.second.tempTextMMR2 += ";ADD_MMR:$additionalMMR"
        }

        //Сохранение в поля
        data.forEach {
            if (it.second.win) {
                it.first.mmrAram = (it.first.mmrAram + it.second.tempTextMMR2value).to1Digits()
            } else {
                it.first.removeMMRvalue(abs(it.second.tempTextMMR2value.to1Digits()))
            }
            it.second.gameMatchMmr = it.second.tempTextMMR2value.to1Digits()
        }

        printLog("MAX MATCH MMR: $maxMMR")
        data.forEach {
            printLog("DATA: ${it.first} ${it.second} win:${it.second.win} ${it.second.tempTextMMR2value} ${it.second.tempTextMMR2}")
        }

        val textMatchResult = "**${pMatch.matchId} ${pMatch.id} ${pMatch.matchMode} ${pMatch.matchDateEnd.toFormatDate()}\n${data.joinToString("\n\t") { it.second.win.toString() + " lol: " + it.first.getCorrectNameWithTag() + " " + it.second.championName + " MMR: " + it.second.gameMatchMmr + " DATA: " + it.second.tempTextMMR2 }}\n**"
        asyncLaunch {
            sqlData.sendMessage(messageId = "", message = textMatchResult)
        }
    }
}