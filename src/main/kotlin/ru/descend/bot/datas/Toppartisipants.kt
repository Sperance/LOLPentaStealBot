package ru.descend.bot.datas

import ru.descend.bot.lowDescriptor
import ru.descend.bot.postgre.R2DBC
import ru.descend.bot.postgre.SQLData_R2DBC
import ru.descend.bot.postgre.r2dbc.model.KORDLOLs
import ru.descend.bot.postgre.r2dbc.model.Matches
import ru.descend.bot.postgre.r2dbc.model.Matches.Companion.tbl_matches
import ru.descend.bot.postgre.r2dbc.model.ParticipantsNew
import ru.descend.bot.toDate
import ru.descend.bot.toFormatDate

data class TopPartObject(
    var lol_id: Int,
    var match_id: Int,
    var stat_name: String,
    var stat_value: Long,
    var stat_champion: String,

    var stat_date_long: Long = 0,
    var stat_date: String = "",
    var stat_lol_name: String = "",
    var match_text: String = "",
) {
    override fun toString(): String {
        val textBold = stat_date_long.toDate().isBeforeDay() || stat_date_long.toDate().isCurrentDay()
        return "${if (textBold) "**" else ""}__${stat_name}:__ $stat_value '$stat_champion' $stat_date |$stat_lol_name|${if (textBold) "**" else ""}"
    }
}

class Toppartisipants {
    private val arrayData = ArrayList<TopPartObject>()

    suspend fun getResults(data: SQLData_R2DBC) : ArrayList<TopPartObject> {
        var matchObj: Matches?
        arrayData.forEach {
            matchObj = Matches().getDataOne(declaration = { tbl_matches.id eq it.match_id })
            it.stat_date = matchObj?.matchDateStart?.toFormatDate()?:""
            it.stat_date_long = matchObj?.matchDateStart?:0
            it.match_text = matchObj?.matchId?:""
            it.stat_lol_name = KORDLOLs().getDataOne({ KORDLOLs.tbl_kordlols.LOL_id eq it.lol_id })?.asUser(data)?.lowDescriptor()?:""
        }
        return arrayData
    }

    suspend fun getResults() : ArrayList<TopPartObject> {
        var matchObj: Matches?
        arrayData.forEach {
            matchObj = Matches().getDataOne(declaration = { tbl_matches.id eq it.match_id })
            it.stat_date = matchObj?.matchDateStart?.toFormatDate()?:""
            it.stat_date_long = matchObj?.matchDateStart?:0
            it.match_text = matchObj?.matchId?:""
            it.stat_lol_name = (KORDLOLs().getDataOne({ KORDLOLs.tbl_kordlols.LOL_id eq it.lol_id })?.LOL_id?:"").toString()
        }
        return arrayData
    }

    suspend fun calculateField(participants: ParticipantsNew, name: String, value: Double) {
        val obj = arrayData.find { it.stat_name == name }
        if (obj != null) {
            if (obj.stat_value < value) {
                obj.lol_id = participants.LOLperson_id
                obj.match_id = participants.match_id
                obj.stat_value = value.toLong()
                obj.stat_champion = R2DBC.getHeroFromNameEN(participants.championName)?.nameRU?:""
            }
        } else {
            arrayData.add(TopPartObject(participants.LOLperson_id, participants.match_id, name, value.toLong(), participants.championName))
        }
    }
}