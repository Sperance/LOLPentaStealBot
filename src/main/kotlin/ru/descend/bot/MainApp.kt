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
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.toList
import me.jakejmattson.discordkt.dsl.bot
import me.jakejmattson.discordkt.extensions.TimeStamp
import ru.descend.bot.lolapi.LeagueMainObject
import ru.descend.bot.postgre.tables.TableGuild
import ru.descend.bot.postgre.tables.TableMatch
import ru.descend.bot.postgre.tables.TableParticipantData
import ru.descend.bot.postgre.Postgre
import ru.descend.bot.postgre.SQLData
import ru.descend.bot.postgre.getGuild
import ru.descend.bot.postgre.kordTemp
import ru.descend.bot.postgre.tables.TableParticipant
import ru.descend.bot.postgre.tables.tableLOLPerson
import ru.descend.bot.postgre.tables.tableMatch
import ru.descend.bot.postgre.tables.tableParticipant
import ru.descend.bot.savedObj.EnumMMRRank
import statements.select
import statements.selectAll
import update
import utils.toJson
import java.awt.Color
import java.lang.ref.WeakReference
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
        val localData = ThreadLocal<SQLData>()
        localData.set(SQLData(guild, getGuild(guild)))
        if (localData.get().guildSQL.botChannelId.isNotEmpty()) {
            localData.get().initDataList()
            showLeagueHistory(localData.get())
            printMemoryUsage("begin clear")
            localData.get()?.clearMainDataList()
            localData.get()?.performClear()
        }
        localData.remove()
        performGC()
        printMemoryUsage("end clear")
        delay(duration)
    }
}

private fun firstInitialize() {
    LeagueMainObject.catchHeroNames()
    Postgre.initializePostgreSQL()
}

suspend fun removeMessage(guild: Guild, guildSQL: TableGuild) {
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

suspend fun showLeagueHistory(sqlData: SQLData) {

    sqlData.resetKORDLOL()
    var isNewMatches = false

    launch {
        sqlData.getKORDLOL().forEach {
            if (it.LOLperson != null && it.LOLperson?.LOL_puuid != null && it.LOLperson?.LOL_puuid != "")
                sqlData.listCurrentUsers.get()?.add(it.LOLperson?.LOL_puuid)
        }
        sqlData.getKORDLOL().forEach {
            if (it.LOLperson == null) return@forEach
            if (it.LOLperson?.LOL_puuid == "") return@forEach
            val checkMatches = ArrayList<String>()
            if (sqlData.listCurrentUsers.get()?.contains(it.LOLperson?.LOL_puuid) == false) {
                printLog(sqlData.guild, "User ${it.LOLperson?.LOL_puuid} ${it.LOLperson?.LOL_summonerName} has skipped match query")
                return@forEach
            }
            LeagueMainObject.catchMatchID(sqlData.guildSQL, it.LOLperson!!.LOL_puuid, 0, 50).forEach ff@{ matchId ->
                    if (!checkMatches.contains(matchId)) checkMatches.add(matchId)
            }
            sqlData.getNewMatches(checkMatches).forEach { newMatch ->
                LeagueMainObject.catchMatch(sqlData.guildSQL, newMatch)?.let { match ->
                    match.info.participants.forEach { part ->
                        if (sqlData.listCurrentUsers.get()?.contains(part.puuid) == true)
                            sqlData.listCurrentUsers.get()?.remove(part.puuid)
                    }
                    isNewMatches = true
                    sqlData.addMatch(match)
                }
            }
        }
    }.join()

    if (isNewMatches) {
        sqlData.resetSavedParticipants()
        sqlData.resetArrayAramMMRData()
        sqlData.resetWinStreak()
        isNewMatches = false
    }

    val channelText: TextChannel = sqlData.guild.getChannelOf<TextChannel>(Snowflake(sqlData.guildSQL.botChannelId))

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

    //Общая статистика по серверу - текст
//    editMessageGlobal(channelText, sqlData.guildSQL.messageId, {
//        editMessageSimpleContent(sqlData, it)
//    }) {
//        createMessageSimple(channelText, sqlData)
//    }
}

suspend fun editMessageGlobal(channelText: TextChannel, messageId: String, editBody: (UserMessageModifyBuilder) -> Unit, createBody: suspend () -> Unit) {
    if (messageId.isBlank()) {
        createBody.invoke()
    } else {
        val message = channelText.getMessageOrNull(Snowflake(messageId))
        if (message != null) message.edit { editBody.invoke(this) }
        else createBody.invoke()
    }
}

suspend fun createMessageMainData(channelText: TextChannel, sqlData: SQLData) {
    val message = channelText.createMessage("Initial Message MainData")
    channelText.getMessage(message.id).edit { editMessageMainDataContent(this, sqlData) }
    sqlData.guildSQL.update (TableGuild::messageIdMain) { messageIdMain = message.id.value.toString() }
}

suspend fun createMessageGlobalStatistic(channelText: TextChannel, sqlData: SQLData) {
    val message = channelText.createMessage("Initial Message GlobalStatistic")
    channelText.getMessage(message.id).edit {
        editMessageGlobalStatisticContent(this, sqlData)
    }
    sqlData.guildSQL.update (TableGuild::messageIdGlobalStatisticData) { messageIdGlobalStatisticData = message.id.value.toString() }
}

suspend fun createMessageSimple(channelText: TextChannel, sqlData: SQLData) {
    val message = channelText.createMessage("Initial Message Simple")
    channelText.getMessage(message.id).edit { editMessageSimpleContent(sqlData,this) }
    sqlData.guildSQL.update (TableGuild::messageId) { messageId = message.id.value.toString() }
}

suspend fun createMessageAramMMRData(channelText: TextChannel, sqlData: SQLData) {
    val message = channelText.createMessage("Initial Message AramMMR")
    channelText.getMessage(message.id).edit { editMessageAramMMRDataContent(this, sqlData) }
    sqlData.guildSQL.update (TableGuild::messageIdArammmr) { messageIdArammmr = message.id.value.toString() }
}

fun editMessageSimpleContent(sqlData: SQLData, builder: UserMessageModifyBuilder) {
    builder.content = "**Статистика по Серверу:** ${TimeStamp.now()}\n" +
            "**Игр на сервере:** ${tableMatch.count { TableMatch::guild eq sqlData.guildSQL }}\n" +
            "**Пользователей в базе:** ${sqlData.getLOL().size}\n" +
            "**Игроков в базе:** ${tableLOLPerson.size}\n" +
            "**Версия игры:** ${LeagueMainObject.LOL_VERSION}\n" +
            "**Количество чемпионов:** ${LeagueMainObject.LOL_HEROES}"

    printLog(sqlData.guild, "[editMessageSimpleContent] completed")
}

fun editMessageGlobalStatisticContent(builder: UserMessageModifyBuilder, sqlData: SQLData) {

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

fun editMessageMainDataContent(builder: UserMessageModifyBuilder, sqlData: SQLData) {

    sqlData.listMainData.get()?.addAll(sqlData.getKORDLOL())

    sqlData.listMainData.get()?.sortBy { it.id }
    val charStr = " / "

    sqlData.clearMainDataList()

    sqlData.mainDataList1.get().addAll(sqlData.listMainData.get()?.map { formatInt(it.id, 2) + charStr + it.asUser(sqlData.guild).lowDescriptor() }?: listOf())
    sqlData.mainDataList2.get().addAll(sqlData.listMainData.get()?.map { it.LOLperson?.LOL_summonerName + "#" + it.LOLperson?.LOL_riotIdTagline }?: listOf())
    sqlData.mainDataList3.get().addAll(sqlData.listMainData.get()?.map { sqlData.getWinStreak()[it.LOLperson?.id] }?: listOf())

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

fun editMessageAramMMRDataContent(builder: UserMessageModifyBuilder, sqlData: SQLData) {

    //Последняя игра АРАМ. Нужна чтобы выделить жирным всех игроков учавствующих в этом матче
    var codeLastAramMatch = tableMatch.select(TableMatch::matchId).where { TableMatch::matchMode eq "ARAM" }.orderByDescending(TableMatch::matchId).limit(1).getEntity()
    val savedArray = sqlData.getArrayAramMMRData()
    savedArray.forEach {
        if (it.match_id == codeLastAramMatch?.matchId) {
            it.bold = true
        }
    }

    val charStr = " / "

    sqlData.clearMainDataList()
    sqlData.mainDataList1.get().addAll(savedArray.map {
        if (it.bold) "**" + formatInt(it.kord_lol_id, 2) + "| " + EnumMMRRank.getMMRRank(it.mmr_aram).nameRank + "**"
        else formatInt(it.kord_lol_id, 2) + "| " + EnumMMRRank.getMMRRank(it.mmr_aram).nameRank
    })
    sqlData.mainDataList2.get().addAll(savedArray.map {
        if (it.bold) "**" + it.mmr_aram + charStr + it.mmr_aram_saved + charStr + it.games + "**"
        else it.mmr_aram.toString() + charStr + it.mmr_aram_saved + charStr + it.games
    })
    sqlData.mainDataList3.get().addAll(savedArray.map {
        if (it.bold) "**" + LeagueMainObject.catchHeroForId(it.champion_id.toString())?.name + charStr + it.mmr + "**"
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