package ru.descend.bot.postgre.calculating

import ru.descend.bot.asyncLaunch
import ru.descend.bot.lolapi.LeagueMainObject
import ru.descend.bot.lolapi.dto.MatchTimelineDTO
import ru.descend.bot.postgre.SQLData_R2DBC
import ru.descend.bot.postgre.R2DBC
import ru.descend.bot.postgre.r2dbc.model.LOLs
import ru.descend.bot.postgre.r2dbc.model.Matches
import ru.descend.bot.postgre.r2dbc.model.Participants.Companion.tbl_participants
import ru.descend.bot.datas.getStrongDate
import ru.descend.bot.lolapi.dto.matchDto.MatchDTO
import ru.descend.bot.sendMessage
import ru.descend.bot.toDate
import ru.descend.bot.writeLog
import java.time.Duration

data class SavedPartSteal(
    var participantId: Long,
    var puuid: String,
    var team: String,
    var timeStamp: Long
)

data class Calc_PentaSteal (
    val sqlData: SQLData_R2DBC,
    val match: MatchDTO,
    val mch: Matches
) {
    suspend fun calculte() {
        if (!isNeedCalc()) return
        calcDataPenta(LeagueMainObject.catchPentaSteal(match.metadata.matchId)).forEach { pair ->

            writeLog(pair.third)

            val firstPart = R2DBC.getParticipantOne({
                tbl_participants.match_id eq mch.id
                tbl_participants.LOLperson_id eq pair.first?.id
            })

            val secondPart = R2DBC.getParticipantOne({
                tbl_participants.match_id eq mch.id
                tbl_participants.LOLperson_id eq pair.second?.id
            })

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
                textPSteal += "PENTASTEAL (${match.metadata.matchId})\nЧел ${firstPart?.LOLpersonObj()?.getCorrectName()} на ${firstPart?.championName} состили Пенту у ${secondPart?.LOLpersonObj()?.getCorrectName()} на ${secondPart?.championName}"
                sqlData.sendMessage(sqlData.guildSQL.messageIdDebug, textPSteal)
            }
        }
    }

    private fun isNeedCalc() : Boolean {
        if (mch.bots) return false
        return match.info.participants.any { it.quadraKills > 0 }
    }

    private suspend fun calcDataPenta(dto: MatchTimelineDTO?) : ArrayList<Triple<LOLs?, LOLs?, String>> {
        val result = ArrayList<Triple<LOLs?, LOLs?, String>>()

        if (dto == null) return result

        val mapPUUID = HashMap<Long, LOLs?>()
        dto.info.participants.forEach { part ->
            mapPUUID[part.participantId] = R2DBC.getLOLs { LOLs.tbl_lols.LOL_puuid eq part.puuid }.firstOrNull()
        }

        var lastDate = System.currentTimeMillis()
        var removedPart: SavedPartSteal? = null
        var isNextCheck = false
        val arrayQuadras = ArrayList<SavedPartSteal>()

        var textLog = ""
        mapPUUID.forEach { (l, loLs) -> textLog += "PART: $l LOL: $loLs\n" }
        textLog += "\n\n"

        dto.info.frames.forEach { frame ->
            frame.events.forEach lets@ { event ->
                if (event.killerId != null && event.type.contains("CHAMPION")) {
                    val betw = Duration.between(lastDate.toDate().toInstant(), event.timestamp.toDate().toInstant())
                    val resDate = getStrongDate(event.timestamp)
                    lastDate = event.timestamp

                    textLog += "${if (event.killerId <= 5) "BLUE" else "RED"} killer:${mapPUUID[event.killerId]?.getCorrectName()}(${event.killerId}) victimId:${mapPUUID[event.victimId]?.getCorrectName()}(${event.victimId}) multiKillLength:${event.multiKillLength ?: 0} type:${event.type} ${resDate.timeSec} STAMP: ${event.timestamp} BETsec: ${betw.toSeconds()}\n"

                    if (isNextCheck && (event.type == "CHAMPION_KILL" || event.type == "CHAMPION_SPECIAL_KILL")) {
                        arrayQuadras.forEach saved@ { sPart ->
                            if (sPart.team == (if (event.killerId <= 5) "BLUE" else "RED") && sPart.participantId != event.killerId && betw.toSeconds() < 60) {
                                textLog += "PENTESTEAL. Чел ${mapPUUID[event.killerId]} состилил Пенту у ${mapPUUID[sPart.participantId]}\n"
                                result.add(Triple(mapPUUID[event.killerId]!!, mapPUUID[sPart.participantId]!!, textLog))
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
                        arrayQuadras.add(SavedPartSteal(event.killerId, mapPUUID[event.killerId]?.LOL_puuid ?: "", if (event.killerId <= 5) "BLUE" else "RED", event.timestamp))
                        isNextCheck = true
                    }
                }
            }
        }

        return result
    }
}