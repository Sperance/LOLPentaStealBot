package ru.descend.bot.postgre

import Entity
import column
import databases.Database
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.getChannelOf
import dev.kord.core.entity.Guild
import dev.kord.core.entity.channel.TextChannel
import ru.descend.bot.launch
import ru.descend.bot.postgre.PostgreSQL.getGuild
import ru.descend.bot.printLog
import ru.descend.bot.savedObj.DataPersonTest
import table
import update

enum class EnumMessageType(val codeMessage: Int, val nameMessage: String) {
    SIMPLE(100, "LOLPentaSteal"), //по умолчанию
    PENTA(101, "Пентакилл") //сообщение о пентакилле
}

data class TableMessage(
    override var id: Int = 0,
    var messageInnerId: String = "",
    var dateTimeSended: Long = 0,
    var key: Int = 0,
    var sended: Boolean = false,

    var guild: TableGuild? = null,
    var KORD_LOL: TableKORD_LOL? = null,
    var match: TableMatch? = null
) : Entity() {
    constructor(type: EnumMessageType, guild: TableGuild, KORD_LOL: TableKORD_LOL? = null, match: TableMatch) : this() {
        this.key = type.codeMessage
        this.guild = guild
        this.KORD_LOL = KORD_LOL
        this.messageInnerId = catchMessageID(match.matchId)
        this.match = match
    }

    private fun catchMessageID(addKey: String) : String {
        return guild?.idGuild + "#" + key + "#" + addKey + "#" + KORD_LOL?.id
    }

    suspend fun sendMessage(text: String, guildDiscord: Guild) {
        if (guild == null) return
        if (sended == true) return
        launch {
            val channelText = guildDiscord.getChannelOf<TextChannel>(Snowflake(guild!!.messageIdStatus))
            channelText.createMessage {
                content = text
            }
        }.invokeOnCompletion {
            update(TableMessage::sended, TableMessage::dateTimeSended){
                sended = true
                dateTimeSended = System.currentTimeMillis()
            }
        }
    }
}

val tableMessage = table<TableMessage, Database> {
    column(TableMessage::messageInnerId).unique()
    column(TableMessage::guild).check { it neq null }
    column(TableMessage::KORD_LOL).check { it neq null }
}