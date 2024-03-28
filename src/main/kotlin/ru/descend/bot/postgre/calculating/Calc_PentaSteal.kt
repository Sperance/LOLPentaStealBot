package ru.descend.bot.postgre.calculating

import ru.descend.bot.asyncLaunch
import ru.descend.bot.lolapi.LeagueMainObject
import ru.descend.bot.lolapi.dataclasses.SavedPartSteal
import ru.descend.bot.lolapi.leaguedata.MatchTimelineDTO
import ru.descend.bot.lolapi.leaguedata.match_dto.MatchDTO
import ru.descend.bot.postgre.SQLData_R2DBC
import ru.descend.bot.postgre.r2dbc.R2DBC
import ru.descend.bot.postgre.r2dbc.model.Matches
import ru.descend.bot.printLog
import ru.descend.bot.savedObj.getStrongDate
import ru.descend.bot.sendMessage
import ru.descend.bot.toDate
import java.time.Duration

data class Calc_PentaSteal (
    val sqlData: SQLData_R2DBC,
    val match: MatchDTO,
    val mch: Matches
) {
    suspend fun calculte() {
        var isNeedCalculate = false
        sqlData.getSavedParticipantsForMatch(mch.id).forEach {
            if (it.kills4 > 0) {
                isNeedCalculate = true
                return@forEach
            }
        }
        if (isNeedCalculate) {
            val parts = mch.getParticipants()
            calcDataPenta(LeagueMainObject.catchPentaSteal(sqlData, match.metadata.matchId)).forEach { pair ->
                val firstPart = parts.find { sqlData.getLOL(it.LOLperson_id)?.LOL_puuid == pair.first }
                val secondPart = parts.find { sqlData.getLOL(it.LOLperson_id)?.LOL_puuid == pair.second }
                var textPSteal = ""

                if (firstPart == null) {
                    textPSteal += "Participant with PUUID ${pair.first} not found in SQL. Match: ${match.metadata.matchId}\n"
                }
                if (secondPart == null) {
                    textPSteal += "Participant with PUUID ${pair.second} not found in SQL. Match: ${match.metadata.matchId}\n"
                }
                if (firstPart == secondPart) {
                    textPSteal += "Participants with PUUID ${pair.first} are Equals. Match: ${match.metadata.matchId}\n"
                }

                asyncLaunch {
                    textPSteal += "PENTASTEAL (${match.metadata.matchId})\nЧел ${sqlData.getLOL(firstPart?.LOLperson_id)?.LOL_riotIdName} на ${firstPart?.championName} состили Пенту у ${sqlData.getLOL(secondPart?.LOLperson_id)?.LOL_riotIdName} на ${secondPart?.championName}"
                    sqlData.sendMessage(sqlData.guildSQL.messageIdDebug, textPSteal)
                    sqlData.sendEmail("PENTASTEAL (${match.metadata.matchId})", "$textPSteal\n\n${pair.third}")
                }
            }
        }
    }

    private fun calcDataPenta(dto: MatchTimelineDTO?) : ArrayList<Triple<String, String, String>> {
        val result = ArrayList<Triple<String, String, String>>()

        if (dto == null) return result

        val mapPUUID = HashMap<Long, String>()
        dto.info.participants.forEach { part ->
            mapPUUID[part.participantId] = part.puuid
        }

        var lastDate = System.currentTimeMillis()
        var removedPart: SavedPartSteal? = null
        var isNextCheck = false
        val arrayQuadras = ArrayList<SavedPartSteal>()
        var mainText = ""

        dto.info.frames.forEach { frame ->
            frame.events.forEach lets@ { event ->
                if (event.killerId != null && event.type.contains("CHAMPION")) {
                    val betw = Duration.between(lastDate.toDate().toInstant(), event.timestamp.toDate().toInstant())
                    val resDate = getStrongDate(event.timestamp)
                    lastDate = event.timestamp

                    val textLog = "EVENT: team:${if (event.killerId <= 5) "BLUE" else "RED"} killerId:${event.killerId} multiKillLength:${event.multiKillLength ?: 0} killType: ${event.killType?:""} type:${event.type} ${resDate.timeSec} STAMP: ${event.timestamp} BETsec: ${betw.toSeconds()}\n"
                    mainText += textLog

                    if (isNextCheck && (event.type == "CHAMPION_KILL" || event.type == "CHAMPION_SPECIAL_KILL")) {
                        arrayQuadras.forEach saved@ { sPart ->
                            if (sPart.team == (if (event.killerId <= 5) "BLUE" else "RED") && sPart.participantId != event.killerId) {
                                printLog("PENTESTEAL. Чел PUUID ${mapPUUID[event.killerId]} состилил Пенту у ${sPart.puuid}")
                                result.add(Triple(mapPUUID[event.killerId]!!, sPart.puuid, mainText))
                                removedPart = sPart
                                return@saved
                            }
                        }
                        if (removedPart != null) {
                            arrayQuadras.remove(removedPart)
                            removedPart = null
                        }
                        isNextCheck = false
                    }
                    if (event.multiKillLength == 4L) {
                        arrayQuadras.add(SavedPartSteal(event.killerId, mapPUUID[event.killerId] ?: "", if (event.killerId <= 5) "BLUE" else "RED", event.timestamp))
                        isNextCheck = true
                    }
                }
            }
        }

        return result
    }
}