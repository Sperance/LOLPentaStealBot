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
import dev.kord.gateway.ALL
import dev.kord.gateway.Intents
import dev.kord.gateway.PrivilegedIntent
import dev.kord.rest.builder.message.embed
import dev.kord.rest.builder.message.modify.UserMessageModifyBuilder
import dev.kord.x.emoji.Emojis
import kotlinx.coroutines.delay
import me.jakejmattson.discordkt.dsl.bot
import me.jakejmattson.discordkt.util.TimeStamp
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
            intents = Intents.ALL
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
                removeMessage(it, R2DBC.getGuild(it))
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
        val localData = SQLData_R2DBC(guild, R2DBC.getGuild(guild))
        if (localData.guildSQL.botChannelId.isNotEmpty()) {
            localData.initialize()
            showLeagueHistory(localData)
            localData.performClear()
        }
        printMemoryUsage("end clear")
        delay(duration)
    }
}

private suspend fun firstInitialize() {
    LeagueMainObject.catchHeroNames()
    R2DBC.initialize()
}

suspend fun removeMessage(guild: Guild, guildSQL: Guilds) {
    if (guildSQL.botChannelId.isNotEmpty()){
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
    sqlData.dataKORD.reset()
    sqlData.dataLOL.reset()

    launch {
        val checkMatches = ArrayList<String>()
        sqlData.dataSavedLOL.get().forEach {
            if (it.LOL_puuid == "") return@forEach
            LeagueMainObject.catchMatchID(sqlData, it.LOL_puuid, it.getCorrectName(), 0, 10).forEach ff@{ matchId ->
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

    val channelText: TextChannel = sqlData.guild.getChannelOf<TextChannel>(Snowflake(sqlData.guildSQL.botChannelId))

    launch {
        //Таблица Главная - ID никнейм серияпобед
        editMessageGlobal(channelText, sqlData.guildSQL.messageIdMain, {
            editMessageMainDataContent(it, sqlData)
        }) {
            createMessageMainData(channelText, sqlData)
        }

        //Таблица ММР - все про ММР арама
        editMessageGlobal(channelText, sqlData.guildSQL.messageIdArammmr, {
            editMessageAramMMRDataContent(it, sqlData)
        }) {
            createMessageAramMMRData(channelText, sqlData)
        }

        //Таблица по играм\винрейту\сериям убийств
        editMessageGlobal(channelText, sqlData.guildSQL.messageIdGlobalStatisticData, {
            editMessageGlobalStatisticContent(it, sqlData)
        }) {
            createMessageGlobalStatistic(channelText, sqlData)
        }
    }.join()
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

suspend fun createMessageAramMMRData(channelText: TextChannel, sqlData: SQLData_R2DBC) {
    val message = channelText.createMessage("Initial Message AramMMR")
    channelText.getMessage(message.id).edit { editMessageAramMMRDataContent(this, sqlData) }

    sqlData.guildSQL.messageIdArammmr = message.id.value.toString()
    sqlData.guildSQL = sqlData.guildSQL.update()
}

suspend fun editMessageGlobalStatisticContent(builder: UserMessageModifyBuilder, sqlData: SQLData_R2DBC) {

    val charStr = " / "
    val savedParts = sqlData.getSavedParticipants()

    val mainDataList1 = (savedParts.map { formatInt(it.kord_lol_id, 2) + "| " + formatInt(it.games, 3) + charStr + formatInt(it.win, 3) + charStr + formatInt(((it.win.toDouble() / it.games.toDouble()) * 100).toInt(), 2) + "%" })
    val mainDataList2 = (savedParts.map {  it.kill.toFormatK() + charStr + formatInt(it.kill3, 3) + charStr + formatInt(it.kill4, 3) + charStr + formatInt(it.kill5, 2) })

    builder.content = "**Статистика Матчей**\nОбновлено: ${TimeStamp.now()}\n"
    builder.embed {
        field {
            name = "Game/Win/WinRate"
            value = mainDataList1.joinToString(separator = "\n")
            inline = true
        }
        field {
            name = "Kill/Triple/Quadra/Penta"
            value = mainDataList2.joinToString(separator = "\n")
            inline = true
        }
    }

    printLog(sqlData.guild, "[editMessageGlobalStatisticContent] completed")
}

suspend fun editMessageMainDataContent(builder: UserMessageModifyBuilder, sqlData: SQLData_R2DBC) {

    sqlData.listMainData.addAll(sqlData.getKORDLOL())

    sqlData.listMainData.sortBy { it.showCode }
    val charStr = " / "

    val mainDataList1 = (sqlData.listMainData.map { formatInt(it.showCode, 2) + charStr + it.asUser(sqlData.guild, sqlData).lowDescriptor() })
    val mainDataList2 = (sqlData.listMainData.map { sqlData.getLOL(it.LOL_id)?.getCorrectName() })
    val mainDataList3 = (sqlData.listMainData.map { sqlData.getWinStreak()[it.LOL_id] })

    builder.content = "**Статистика Главная**\nОбновлено: ${TimeStamp.now()}\n"
    builder.embed {
        field {
            name = "ID/User"
            value = mainDataList1.joinToString(separator = "\n")
            inline = true
        }
        field {
            name = "Nickname"
            value = mainDataList2.joinToString(separator = "\n")
            inline = true
        }
        field {
            name = "WinStreak"
            value = mainDataList3.joinToString(separator = "\n")
            inline = true
        }
    }

    printLog(sqlData.guild, "[editMessageMainDataContent] completed")
}

suspend fun editMessageAramMMRDataContent(builder: UserMessageModifyBuilder, sqlData: SQLData_R2DBC) {

    val charStr = " / "
    val aramData = sqlData.getArrayAramMMRData()

    val mainDataList1 = (aramData.map {
        if (it.match_id == it.last_match_id) "**" + formatInt(it.kord_lol_id, 2) + "| " + EnumMMRRank.getMMRRank(it.mmr_aram).nameRank + "**"
        else formatInt(it.kord_lol_id, 2) + "| " + EnumMMRRank.getMMRRank(it.mmr_aram).nameRank
    })
    val mainDataList2 = (aramData.map {
        if (it.match_id == it.last_match_id) "**" + it.mmr_aram + charStr + it.mmr_aram_saved + charStr + it.games + "**"
        else it.mmr_aram.toString() + charStr + it.mmr_aram_saved + charStr + it.games
    })
    val mainDataList3 = (aramData.map {
        if (it.match_id == it.last_match_id) "**" + LeagueMainObject.catchHeroForId(it.champion_id.toString())?.name + charStr + it.mmr + "**"
        else LeagueMainObject.catchHeroForId(it.champion_id.toString())?.name + charStr + it.mmr
    })

    builder.content = "**Статистика ММР**\nОбновлено: ${TimeStamp.now()}\n"
    builder.embed {
        field {
            name = "ARAM Rank"
            value = mainDataList1.joinToString(separator = "\n")
            inline = true
        }
        field {
            name = "MMR/Bonus/Games"
            value = mainDataList2.joinToString(separator = "\n")
            inline = true
        }
        field {
            name = "LastGame/MMR"
            value = mainDataList3.joinToString(separator = "\n")
            inline = true
        }
    }

    printLog(sqlData.guild, "[editMessageAramMMRDataContent] completed")
}