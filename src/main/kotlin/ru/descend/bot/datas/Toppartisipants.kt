package ru.descend.bot.datas

import ru.descend.bot.lolapi.LeagueMainObject
import ru.descend.bot.postgre.r2dbc.R2DBC
import ru.descend.bot.postgre.r2dbc.model.LOLs
import ru.descend.bot.postgre.r2dbc.model.Matches
import ru.descend.bot.postgre.r2dbc.model.Participants
import ru.descend.bot.savedObj.isBeforeDay
import ru.descend.bot.to1Digits
import ru.descend.bot.toDate
import ru.descend.bot.toFormatDate

data class TopPartObject(
    var lol_id: Int,
    var match_id: Int,
    var stat_name: String,
    var stat_value: Double,
    var stat_champion: String,

    var stat_date_long: Long = 0,
    var stat_date: String = "",
    var stat_lol_name: String = ""
) {
    override fun toString(): String {
        val textBold = stat_date_long.toDate().isBeforeDay()
        return "${if (textBold) "**" else ""}__${stat_name}:__ ${stat_value.to1Digits()} '${LeagueMainObject.catchHeroForName(stat_champion)?.name?:stat_champion}' $stat_date $stat_lol_name${if (textBold) "**" else ""}"
    }
}

class Toppartisipants {
    private val arrayData = ArrayList<TopPartObject>()

    suspend fun getResults() : ArrayList<TopPartObject> {
        arrayData.forEach {
            val matchObj = R2DBC.getMatches { Matches.tbl_matches.id eq it.match_id }.firstOrNull()
            it.stat_date = matchObj?.matchDateStart?.toFormatDate()?:""
            it.stat_date_long = matchObj?.matchDateStart?:0
            it.stat_lol_name = R2DBC.getLOLs { LOLs.tbl_lols.id eq it.lol_id }.firstOrNull()?.getCorrectName()?:""
        }
        return arrayData
    }

    fun calculateField(participants: Participants, name: String, value: Double) {
        val obj = arrayData.find { it.stat_name == name }
        if (obj != null) {
            if (obj.stat_value < value) {
                obj.lol_id = participants.LOLperson_id
                obj.match_id = participants.match_id
                obj.stat_value = value
                obj.stat_champion = participants.championName
            }
        } else {
            arrayData.add(TopPartObject(participants.LOLperson_id, participants.match_id, name, value, participants.championName))
        }
    }
}