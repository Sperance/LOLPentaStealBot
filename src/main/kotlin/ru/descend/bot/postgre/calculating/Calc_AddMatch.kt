package ru.descend.bot.postgre.calculating

import org.komapper.core.dsl.QueryDsl
import ru.descend.bot.LOAD_MMR_HEROES_MATCHES
import ru.descend.bot.LVP_TAG
import ru.descend.bot.MVP_TAG
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
import ru.descend.bot.to1Digits
import ru.descend.bot.toFormatDate
import ru.descend.bot.writeLog
import kotlin.math.abs

data class Calc_AddMatch (
    val sqlData: SQLData_R2DBC,
    val match: MatchDTO
) {

    val arrayOtherLOLs = ArrayList<LOLs>()

    suspend fun calculate(mainOrder: Boolean) : Matches {
        arrayOtherLOLs.clear()
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
        ).create(Matches::matchId, "[atom:${sqlData.atomicIntLoaded.get()}]")

        if (!pMatchResult.bit) return pMatchResult.result
        val pMatch = pMatchResult.result

        if (pMatch.matchMode == "ARAM" && mainOrder) sqlData.isHaveLastARAM = true

        if (pMatch.id % LOAD_MMR_HEROES_MATCHES == 0){
            R2DBC.executeProcedure("call \"GetAVGs\"()")
            LeagueMainObject.catchHeroNames()
        }

        val arrayHeroName = ArrayList<Participant>()
        match.info.participants.forEach {part ->
            arrayHeroName.add(part)
        }

        val savedLOL = sqlData.dataSavedLOL.get()
        val curLOLs = LOLs().getData({ tbl_lols.LOL_puuid.inList(arrayHeroName.map { it.puuid }) })
        val arrayNewParts = ArrayList<ParticipantsNew>()
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
                    profile_icon = part.profileIcon,
                    match_date_last = pMatch.matchDateEnd).create(LOLs::LOL_puuid).result
            } else if (!curLOL.isBot() && curLOL.isNeedUpdate(pMatch, part)) {
                curLOL.LOL_riotIdTagline = part.riotIdTagline
                curLOL.LOL_region = pMatch.getRegionValue()
                curLOL.LOL_summonerId = part.summonerId
                val newName = if (part.riotIdGameName == "null") part.summonerName else part.riotIdGameName
                if (newName != "null") curLOL.LOL_riotIdName = newName
                curLOL.LOL_summonerLevel = part.summonerLevel
                curLOL.profile_icon = part.profileIcon
                curLOL.match_date_last = pMatch.matchDateEnd
                curLOL = curLOL.update()
            } else if (!curLOL.isBot() && pMatch.matchDateEnd > curLOL.match_date_last) {
                curLOL.match_date_last = pMatch.matchDateEnd
                curLOL = curLOL.update()
            }

            lastLolsList.add(curLOL)

            if (!curLOL.isBot() && sqlData.dataKORDLOL.get().find { tbl -> tbl.LOL_id == curLOL.id } == null)
                arrayOtherLOLs.add(curLOL)

            if (pMatch.isNeedCalcStats()) arrayNewParts.add(ParticipantsNew(part, pMatch, curLOL))
        }

        arrayOtherLOLs.removeIf { savedLOL.find { finded -> finded.id == it.id } != null }

        if (pMatch.isNeedCalcStats()) {
            val lastPartList = db.runQuery { QueryDsl.insert(tbl_participantsnew).multiple(arrayNewParts) }
            calculateMMR(pMatch, lastPartList, lastLolsList, mainOrder)
        }

        return pMatch
    }

    private suspend fun calculateMMR(pMatch: Matches, lastPartList: List<ParticipantsNew>, lastLolsList: List<LOLs>, mainOrder: Boolean) {
        val arrayKORDmmr = ArrayList<Pair<LOLs, ParticipantsNew>>()
        if (pMatch.isNeedCalcMMR()) {
            val data = Calc_MMR(lastPartList, pMatch)
            data.calculateMMR()
            lastLolsList.forEach {
                val finededPart = lastPartList.find { par -> par.LOLperson_id == it.id }
                if (finededPart != null) {
                    arrayKORDmmr.add(Pair(it, finededPart))
                }
            }
        }

        val textMatch: String
        if (arrayKORDmmr.isNotEmpty() && pMatch.isNeedCalcMMR()) {
            textMatch = calcMMR20(pMatch, arrayKORDmmr)
            arrayKORDmmr.forEach {
                it.first.update()
                it.second.update()
            }
        } else {
            textMatch = if (mainOrder) "${pMatch.matchId} ${pMatch.id} ${pMatch.matchMode} ${pMatch.matchDateEnd.toFormatDate()}\n${lastPartList.joinToString { it.win.toString() + " lol: " + it.LOLperson_id + " " + it.championName + "\n" }}"
            else "${pMatch.matchId} ${pMatch.id} ${pMatch.matchMode} ${pMatch.matchDateEnd.toFormatDate()}\n"
        }

        sqlData.textNewMatches.appendLine(textMatch)
    }

    private fun calcMMR20(pMatch: Matches, data: ArrayList<Pair<LOLs, ParticipantsNew>>) : String {

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
            it.second.tempTextMMR2value += 10
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
            additionalMMR += it.second.kills4 * 6.0
            additionalMMR += it.second.tookLargeDamageSurvived * 3.0
            if (it.second.win) additionalMMR += 2.0
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

        return "**${pMatch.matchId} ${pMatch.id} ${pMatch.matchMode} ${pMatch.matchDateEnd.toFormatDate()}\n${data.joinToString("\n\t") { it.second.win.toString() + " lol: " + it.first.getCorrectNameWithTag() + " " + it.second.championName + " MMR: " + it.second.gameMatchMmr + " DATA: " + it.second.tempTextMMR2 }}\n**"
    }
}