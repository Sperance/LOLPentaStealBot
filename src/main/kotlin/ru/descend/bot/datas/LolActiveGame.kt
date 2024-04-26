package ru.descend.bot.datas

import ru.descend.bot.lolapi.dto.currentGameInfo.Participant
import ru.descend.bot.postgre.r2dbc.model.KORDLOLs
import ru.descend.bot.postgre.r2dbc.model.LOLs

data class LolActiveGame(
    val lol: LOLs? = null,
    val kordlol: KORDLOLs? = null,
    val part: Participant,
    val matchId: Int,
    val messageId: Long? = null
)