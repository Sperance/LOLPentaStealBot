package ru.descend.bot

import dev.kord.common.annotation.KordPreview
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.common.entity.PresenceStatus
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.getChannelOf
import dev.kord.core.entity.Guild
import dev.kord.core.entity.channel.TextChannel
import dev.kord.gateway.Intents
import dev.kord.gateway.PrivilegedIntent
import dev.kord.rest.builder.message.modify.UserMessageModifyBuilder
import dev.kord.rest.builder.message.modify.embed
import dev.kord.x.emoji.Emojis
import kotlinx.coroutines.delay
import me.jakejmattson.discordkt.dsl.bot
import me.jakejmattson.discordkt.extensions.TimeStamp
import ru.descend.bot.lolapi.LeagueMainObject
import ru.descend.bot.postgre.SQLData_R2DBC
import ru.descend.bot.postgre.r2dbc.R2DBC
import ru.descend.bot.postgre.r2dbc.model.Guilds
import ru.descend.bot.savedObj.EnumMMRRank
import java.awt.Color
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

@OptIn(PrivilegedIntent::class)
@KordPreview
fun main() {
    bot(catchToken()[0]) {
        prefix {
            "ll."
        }
        configure {
            logStartup = true
            documentCommands = true
            recommendCommands = true
            searchCommands = true
            deleteInvocation = true
            dualRegistry = true
            commandReaction = Emojis.adult
            theme = Color(0x00B92F)
            intents = Intents.all
            defaultPermissions = Permissions(Permission.UseApplicationCommands)
        }
        onException {
            println("Exception '${exception::class.simpleName}': ${exception.message}")
        }
        presence {
            this.status = PresenceStatus.Online
            playing("ARAM")
        }
        onStart {
            firstInitialize()
            kord.guilds.collect {
                if (it.id.value.toString() == "1160986529460654111")
                    return@collect

                timerRequestReset((2).minutes)
                timerMainInformation(it, (5).minutes)
            }
        }
    }
}

fun timerRequestReset(duration: Duration) = launch {
    while (true) {
        globalLOLRequests = 0
        delay(duration)
    }
}

fun timerMainInformation(guild: Guild, duration: Duration) = launch {
    while (true) {
        val localData = ThreadLocal<SQLData_R2DBC>()
        localData.set(SQLData_R2DBC(guild, R2DBC.getGuild(guild)))
        if (localData.get().guildSQL.botChannelId.isNotEmpty()) {
            localData.get().initialize()
            showLeagueHistory(localData.get())
            localData.get()?.clearMainDataList()
            localData.get()?.performClear()
        }
        localData.remove()
        performGC()
        printMemoryUsage("end clear")
        delay(duration)
    }
}

private suspend fun firstInitialize() {
    LeagueMainObject.catchHeroNames()
    R2DBC.initialize()
}

suspend fun removeMessage(guild: Guild, guildSQL: Guilds) {
    if (guildSQL.botChannelId.isNotEmpty()) {
        guild.getChannelOf<TextChannel>(Snowflake(guildSQL.botChannelId)).messages.collect {
            if (it.id.value.toString() in listOf(guildSQL.messageId, guildSQL.messageIdPentaData, guildSQL.messageIdGlobalStatisticData, guildSQL.messageIdMasteryData, guildSQL.messageIdMain, guildSQL.messageIdArammmr)) {
                Unit
            } else {
                it.delete()
            }
        }
    }
}

var globalLOLRequests = 0
var statusLOLRequests = 0

suspend fun showLeagueHistory(sqlData: SQLData_R2DBC) {

    sqlData.dataKORDLOL.reset()

    launch {
        val checkMatches = ArrayList<String>()
        sqlData.dataSavedLOL.get().forEach {
            if (it.LOL_puuid == "") return@forEach
            LeagueMainObject.catchMatchID(sqlData, it.LOL_puuid, it.LOL_riotIdName?:it.LOL_summonerName, 5, 5).forEach ff@{ matchId ->
                if (!checkMatches.contains(matchId)) checkMatches.add(matchId)
            }
        }
        val listChecked = sqlData.getNewMatches(checkMatches)
        listChecked.sortBy { it }
        listChecked.forEach { newMatch ->
            LeagueMainObject.catchMatch(sqlData, newMatch)?.let { match ->
                sqlData.addMatch(match)
            }
        }
    }.join()

//    sqlData.resetSavedParticipants()
//    sqlData.resetArrayAramMMRData()
//    sqlData.resetWinStreak()
//
//    val channelText: TextChannel = sqlData.guild.getChannelOf<TextChannel>(Snowflake(sqlData.guildSQL.botChannelId))
//
//    //Таблица Главная - ID никнейм серияпобед
//    editMessageGlobal(channelText, sqlData.guildSQL.messageIdMain, {
//        editMessageMainDataContent(it, sqlData)
//    }) {
//        createMessageMainData(channelText, sqlData)
//    }
//
//    //Таблица ММР - все про ММР арама
//    editMessageGlobal(channelText, sqlData.guildSQL.messageIdArammmr, {
//        editMessageAramMMRDataContent(it, sqlData)
//    }) {
//        createMessageAramMMRData(channelText, sqlData)
//    }
//
//    //Таблица по играм\винрейту\сериям убийств
//    editMessageGlobal(channelText, sqlData.guildSQL.messageIdGlobalStatisticData, {
//        editMessageGlobalStatisticContent(it, sqlData)
//    }) {
//        createMessageGlobalStatistic(channelText, sqlData)
//    }
}

suspend fun editMessageGlobal(channelText: TextChannel, messageId: String, editBody: suspend (UserMessageModifyBuilder) -> Unit, createBody: suspend () -> Unit) {
    if (messageId.isBlank()) {
        createBody.invoke()
    } else {
        val message = channelText.getMessageOrNull(Snowflake(messageId))
        if (message != null) message.edit { editBody.invoke(this) }
        else createBody.invoke()
    }
}

suspend fun createMessageMainData(channelText: TextChannel, sqlData: SQLData_R2DBC) {
    val message = channelText.createMessage("Initial Message MainData")
    channelText.getMessage(message.id).edit { editMessageMainDataContent(this, sqlData) }

    sqlData.guildSQL.messageIdMain = message.id.value.toString()
    sqlData.guildSQL = sqlData.guildSQL.update()
}

suspend fun createMessageGlobalStatistic(channelText: TextChannel, sqlData: SQLData_R2DBC) {
    val message = channelText.createMessage("Initial Message GlobalStatistic")
    channelText.getMessage(message.id).edit {
        editMessageGlobalStatisticContent(this, sqlData)
    }

    sqlData.guildSQL.messageIdGlobalStatisticData = message.id.value.toString()
    sqlData.guildSQL = sqlData.guildSQL.update()
}

//suspend fun createMessageSimple(channelText: TextChannel, sqlData: SQLData) {
//    val message = channelText.createMessage("Initial Message Simple")
//    channelText.getMessage(message.id).edit { editMessageSimpleContent(sqlData,this) }
//    sqlData.guildSQL.update (TableGuild::messageId) { messageId = message.id.value.toString() }
//}

suspend fun createMessageAramMMRData(channelText: TextChannel, sqlData: SQLData_R2DBC) {
    val message = channelText.createMessage("Initial Message AramMMR")
    channelText.getMessage(message.id).edit { editMessageAramMMRDataContent(this, sqlData) }

    sqlData.guildSQL.messageIdArammmr = message.id.value.toString()
    sqlData.guildSQL = sqlData.guildSQL.update()
}

//fun editMessageSimpleContent(sqlData: SQLData, builder: UserMessageModifyBuilder) {
//    builder.content = "**Статистика по Серверу:** ${TimeStamp.now()}\n" +
//            "**Пользователей в базе:** ${sqlData.getKORDLOL().size}\n" +
//            "**Версия игры:** ${LeagueMainObject.LOL_VERSION}\n" +
//            "**Количество чемпионов:** ${LeagueMainObject.LOL_HEROES}"
//
//    printLog(sqlData.guild, "[editMessageSimpleContent] completed")
//}

suspend fun editMessageGlobalStatisticContent(builder: UserMessageModifyBuilder, sqlData: SQLData_R2DBC) {

    val charStr = " / "

    sqlData.clearMainDataList()

    sqlData.mainDataList1.get()?.addAll(sqlData.getSavedParticipants().map { formatInt(it.kord_lol_id, 2) + "| " + formatInt(it.games, 3) + charStr + formatInt(it.win, 3) + charStr + formatInt(((it.win.toDouble() / it.games.toDouble()) * 100).toInt(), 2) + "%" })
    sqlData.mainDataList2.get()?.addAll(sqlData.getSavedParticipants().map {  it.kill.toFormatK() + charStr + formatInt(it.kill3, 3) + charStr + formatInt(it.kill4, 3) + charStr + formatInt(it.kill5, 2) })

    builder.content = "**Статистика Матчей**\nОбновлено: ${TimeStamp.now()}\n"
    builder.embed {
        field {
            name = "Game/Win/WinRate"
            value = sqlData.mainDataList1.get()?.joinToString(separator = "\n").toString()
            inline = true
        }
        field {
            name = "Kill/Triple/Quadra/Penta"
            value = sqlData.mainDataList2.get().joinToString(separator = "\n")
            inline = true
        }
    }

    printLog(sqlData.guild, "[editMessageGlobalStatisticContent] completed")
}

suspend fun editMessageMainDataContent(builder: UserMessageModifyBuilder, sqlData: SQLData_R2DBC) {

    sqlData.listMainData.addAll(sqlData.getKORDLOL())

    sqlData.listMainData.sortBy { it.id }
    val charStr = " / "

    sqlData.clearMainDataList()

    sqlData.mainDataList1.get().addAll(sqlData.listMainData.map { formatInt(it.id, 2) + charStr + it.asUser(sqlData.guild, sqlData).lowDescriptor() })
    sqlData.mainDataList2.get().addAll(sqlData.listMainData.map { it.getNickNameWithTag(sqlData) })
    sqlData.mainDataList3.get().addAll(sqlData.listMainData.map { sqlData.getWinStreak()[it.LOL_id] })

    builder.content = "**Статистика Главная**\nОбновлено: ${TimeStamp.now()}\n"
    builder.embed {
        field {
            name = "ID/User"
            value = sqlData.mainDataList1.get().joinToString(separator = "\n")
            inline = true
        }
        field {
            name = "Nickname"
            value = sqlData.mainDataList2.get().joinToString(separator = "\n")
            inline = true
        }
        field {
            name = "WinStreak"
            value = sqlData.mainDataList3.get().joinToString(separator = "\n")
            inline = true
        }
    }

    printLog(sqlData.guild, "[editMessageMainDataContent] completed")
}

suspend fun editMessageAramMMRDataContent(builder: UserMessageModifyBuilder, sqlData: SQLData_R2DBC) {

    val charStr = " / "

    sqlData.clearMainDataList()
    sqlData.mainDataList1.get().addAll(sqlData.getArrayAramMMRData().map {
        if (it.match_id == it.last_match_id) "**" + formatInt(it.kord_lol_id, 2) + "| " + EnumMMRRank.getMMRRank(it.mmr_aram).nameRank + "**"
        else formatInt(it.kord_lol_id, 2) + "| " + EnumMMRRank.getMMRRank(it.mmr_aram).nameRank
    })
    sqlData.mainDataList2.get().addAll(sqlData.getArrayAramMMRData().map {
        if (it.match_id == it.last_match_id) "**" + it.mmr_aram + charStr + it.mmr_aram_saved + charStr + it.games + "**"
        else it.mmr_aram.toString() + charStr + it.mmr_aram_saved + charStr + it.games
    })
    sqlData.mainDataList3.get().addAll(sqlData.getArrayAramMMRData().map {
        if (it.match_id == it.last_match_id) "**" + LeagueMainObject.catchHeroForId(it.champion_id.toString())?.name + charStr + it.mmr + "**"
        else LeagueMainObject.catchHeroForId(it.champion_id.toString())?.name + charStr + it.mmr
    })

    builder.content = "**Статистика ММР**\nОбновлено: ${TimeStamp.now()}\n"
    builder.embed {
        field {
            name = "ARAM Rank"
            value = sqlData.mainDataList1.get().joinToString(separator = "\n")
            inline = true
        }
        field {
            name = "MMR/Bonus/Games"
            value = sqlData.mainDataList2.get().joinToString(separator = "\n")
            inline = true
        }
        field {
            name = "LastGame/MMR"
            value = sqlData.mainDataList3.get().joinToString(separator = "\n")
            inline = true
        }
    }

    printLog(sqlData.guild, "[editMessageAramMMRDataContent] completed")
}