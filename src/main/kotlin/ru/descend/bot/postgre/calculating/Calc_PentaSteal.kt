package ru.descend.bot.postgre.calculating

import ru.descend.bot.lolapi.LeagueMainObject
import ru.descend.bot.lolapi.leaguedata.match_dto.MatchDTO
import ru.descend.bot.postgre.SQLData_R2DBC
import ru.descend.bot.postgre.r2dbc.model.Matches

data class Calc_PentaSteal (
    val sqlData: SQLData_R2DBC,
    val match: MatchDTO,
    val mch: Matches
) {
    suspend fun calculte() {
        var isNeedCalculate = false
        match.info.participants.forEach {
            if (it.quadraKills - it.pentaKills > 0) {
                isNeedCalculate = true
                return@forEach
            }
        }
        if (isNeedCalculate) {
            val parts = sqlData.getParticipantsForMatch(mch.id)
            LeagueMainObject.catchPentaSteal(match.metadata.matchId).forEach { pair ->
                val firstPart = parts.find { sqlData.getLOL(it.LOLperson_id)?.LOL_puuid == pair.first }
                val secondPart = parts.find { sqlData.getLOL(it.LOLperson_id)?.LOL_puuid == pair.second }

                if (firstPart == null) {
                    sqlData.sendEmail("Error", "Participant with PUUID ${pair.first} not found in SQL. Match:${match.metadata.matchId}")
                    return
                }

                if (secondPart == null) {
                    sqlData.sendEmail("Error", "Participant with PUUID ${pair.second} not found in SQL. Match:${match.metadata.matchId}")
                    return
                }

                if (firstPart == secondPart) {
                    sqlData.sendEmail("Error", "Participants with PUUID ${pair.first} are Equals. Match:${match.metadata.matchId}")
                    return
                }

                if (sqlData.getKORDLOL().find { kl -> kl.LOL_id == firstPart.LOLperson_id } == null && sqlData.getKORDLOL().find { kl -> kl.LOL_id == secondPart.LOLperson_id } == null) {
                    sqlData.sendEmail("PENTASTEAL (${match.metadata.matchId})", "ХЗ какой чел ${sqlData.getLOL(firstPart.LOLperson_id)?.LOL_riotIdName} на ${firstPart.championName} состили Пенту у хз кого ${sqlData.getLOL(secondPart.LOLperson_id)?.LOL_riotIdName} на ${secondPart.championName}\n\n${pair.third}")
                    return
                }

                sqlData.sendEmail("PENTASTEAL (${match.metadata.matchId})", "Чел ${sqlData.getLOL(firstPart.LOLperson_id)?.LOL_riotIdName} на ${firstPart.championName} состили Пенту у ${sqlData.getLOL(secondPart.LOLperson_id)?.LOL_riotIdName} на ${secondPart.championName}\n\n${pair.third}")
            }
        }
    }
}