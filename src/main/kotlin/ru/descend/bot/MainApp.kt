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
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.joinAll
import me.jakejmattson.discordkt.dsl.bot
import me.jakejmattson.discordkt.extensions.TimeStamp
import ru.descend.bot.lolapi.LeagueMainObject
import ru.descend.bot.lolapi.champions.ChampionsDTO
import ru.descend.bot.lolapi.leaguedata.championMasteryDto.ChampionMasteryDto
import ru.descend.bot.postgre.tables.TableGuild
import ru.descend.bot.postgre.tables.TableKORD_LOL
import ru.descend.bot.postgre.tables.TableMatch
import ru.descend.bot.postgre.tables.TableParticipantData
import ru.descend.bot.postgre.Postgre
import ru.descend.bot.postgre.SQLData
import ru.descend.bot.postgre.getGuild
import ru.descend.bot.postgre.tables.TableParticipant
import ru.descend.bot.postgre.tables.tableLOLPerson
import ru.descend.bot.postgre.tables.tableMatch
import ru.descend.bot.postgre.tables.tableParticipant
import ru.descend.bot.savedObj.DataBasic
import ru.descend.bot.savedObj.EnumMMRRank
import statements.selectAll
import update
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
            kord.guilds.toList().forEach {
                isWorkMainThread[it] = true

                if (it.id.value.toString() == "1160986529460654111")
                    return@forEach

                timerRequestReset((2).minutes)
                timerMainInformation(it, (5).minutes)
            }
        }
    }
}

fun firstLoadData(guild: Guild) = launch {
    val guildSQL = getGuild(guild)
    removeMessage(guild, guildSQL)
    val data = SQLData(guild, guildSQL)
    mainMapData[guild] = data
}

fun timerRequestReset(duration: Duration) = launch {
    while (true) {
        globalLOLRequests = 0
        delay(duration)
    }
}

fun timerMainInformation(it: Guild, duration: Duration) = launch {
    while (true) {
        firstLoadData(it).join()
        if (mainMapData[it]?.guildSQL?.botChannelId?.isNotEmpty() == true) {
            showLeagueHistory(it)
        }
        delay(duration)
    }
}

private fun firstInitialize() {
    LeagueMainObject.catchHeroNames()
    Postgre.initializePostgreSQL()
}

suspend fun removeMessage(guild: Guild, guildSQL: TableGuild) {
    if (guildSQL.botChannelId.isNotEmpty()) {
        val channelText = guild.getChannelOf<TextChannel>(Snowflake(guildSQL.botChannelId))
        channelText.messages.collect {
            val msgId = it.id.value.toString()
            if (msgId in listOf(guildSQL.messageId, guildSQL.messageIdPentaData, guildSQL.messageIdGlobalStatisticData, guildSQL.messageIdMasteryData, guildSQL.messageIdMain, guildSQL.messageIdArammmr)) {
                Unit
            } else {
                it.delete()
            }
        }
    }
}

var globalLOLRequests = 0
var statusLOLRequests = 0

var isWorkMainThread = HashMap<Guild, Boolean>()
var mainMapData = HashMap<Guild, SQLData>()

suspend fun showLeagueHistory(guild: Guild) {

    val sqlData = mainMapData[guild]

    if (sqlData == null) {
        printLog(guild, "[MainApp] sqlData is null")
        return
    }

    if (isWorkMainThread[sqlData.guild]!! == false) {
        printLog(sqlData.guild, "[MainApp] isWorkMainThread false")
        return
    }

    var kordlol = sqlData.getKORDLOL()
    val savedParts = sqlData.getSavedParticipants()

    val tableMMR = sqlData.getMMR()
    val currentUsers = ArrayList<String?>()
    launch {
        kordlol.forEach {
            if (it.LOLperson != null && it.LOLperson?.LOL_puuid != null && it.LOLperson?.LOL_puuid != "")
                currentUsers.add(it.LOLperson?.LOL_puuid)
        }
        kordlol.forEach {
            if (it.LOLperson == null) return@forEach
            if (it.LOLperson?.LOL_puuid == "") return@forEach
            val checkMatches = ArrayList<String>()
            if (!currentUsers.contains(it.LOLperson?.LOL_puuid)){
                printLog(guild, "User ${it.LOLperson?.LOL_puuid} ${it.LOLperson?.LOL_summonerName} has skipped match query")
                return@forEach
            }
            LeagueMainObject.catchMatchID(sqlData.guildSQL, it.LOLperson!!.LOL_puuid, 0,50).forEach ff@ { matchId ->
                if (!checkMatches.contains(matchId)) checkMatches.add(matchId)
            }
            sqlData.getNewMatches(checkMatches).forEach {newMatch ->
                LeagueMainObject.catchMatch(sqlData.guildSQL, newMatch)?.let { match ->
                    match.info.participants.forEach { part ->
                        if (currentUsers.contains(part.puuid)) {
                            currentUsers.remove(part.puuid)
                        }
                    }
                    sqlData.addMatch(match, kordlol, tableMMR)
                }
            }
        }
    }.join()
    kordlol = sqlData.getKORDLOL()

    val winStreakMap = catchWinStreak(sqlData, kordlol)
    val channelText = sqlData.guild.getChannelOf<TextChannel>(Snowflake(sqlData.guildSQL.botChannelId))

    //Таблица Главная - ID никнейм серияпобед
    editMessageGlobal(channelText, sqlData.guildSQL.messageIdMain, {
        editMessageMainDataContent(it, sqlData, kordlol, winStreakMap)
    }) {
        createMessageMainData(channelText, sqlData, kordlol, winStreakMap)
    }

    //Таблица ММР - все про ММР арама
    editMessageGlobal(channelText, sqlData.guildSQL.messageIdArammmr, {
        editMessageAramMMRDataContent(it, sqlData, kordlol)
    }) {
        createMessageAramMMRData(channelText, sqlData, kordlol)
    }

    //Таблица Пентакиллов - пента дата
//    editMessageGlobal(channelText, sqlData.guildSQL.messageIdPentaData, {
//        editMessagePentaDataContent(it, sqlData, kordlol, savedParts)
//    }) {
//        createMessagePentaData(channelText, sqlData, kordlol, savedParts)
//    }
//    joinAll()

    //Таблица по играм\винрейту\сериям убийств
    editMessageGlobal(channelText, sqlData.guildSQL.messageIdGlobalStatisticData, {
        editMessageGlobalStatisticContent(it, sqlData, kordlol, savedParts)
    }) {
        createMessageGlobalStatistic(channelText, sqlData, kordlol, savedParts)
    }

    //Общая статистика по серверу - текст
    editMessageGlobal(channelText, sqlData.guildSQL.messageId, {
        editMessageSimpleContent(sqlData, it)
    }) {
        createMessageSimple(channelText, sqlData)
    }
}

fun catchWinStreak(sqlData: SQLData, kordLol: List<TableKORD_LOL>): HashMap<TableKORD_LOL, Int> {

    val mapResult = HashMap<TableKORD_LOL, Int>()

    //Инициализация
    kordLol.forEach {
        var counter = 0
        sqlData.getLastParticipants(it.LOLperson?.LOL_puuid, 10).forEach {objectPerson ->
            if (objectPerson.win) {
                if (counter < 0) counter = 0
                counter++
            } else {
                if (counter > 0) counter = 0
                counter--
            }
        }

        mapResult[it] = counter
    }

    printLog(sqlData.guild, "[catchWinStreak] completed")
    return mapResult
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

suspend fun createMessageMainData(channelText: TextChannel, sqlData: SQLData, kordLol: List<TableKORD_LOL>, mapWins: HashMap<TableKORD_LOL, Int>) {
    val message = channelText.createMessage("Initial Message MainData")
    channelText.getMessage(message.id).edit { editMessageMainDataContent(this, sqlData, kordLol, mapWins) }
    sqlData.guildSQL.update (TableGuild::messageIdMain) { messageIdMain = message.id.value.toString() }
}

suspend fun createMessagePentaData(channelText: TextChannel, sqlData: SQLData, kordLol: List<TableKORD_LOL>, savedParts: List<TableParticipant>) {
    val message = channelText.createMessage("Initial Message PentaData")
    channelText.getMessage(message.id).edit { editMessagePentaDataContent(this, sqlData, kordLol, savedParts) }
    sqlData.guildSQL.update (TableGuild::messageIdPentaData) { messageIdPentaData = message.id.value.toString() }
}

suspend fun createMessageGlobalStatistic(channelText: TextChannel, sqlData: SQLData, kordLol: List<TableKORD_LOL>, savedParts: List<TableParticipant>) {
    val message = channelText.createMessage("Initial Message GlobalStatistic")
    channelText.getMessage(message.id).edit {
        editMessageGlobalStatisticContent(this, sqlData, kordLol, savedParts)
    }
    sqlData.guildSQL.update (TableGuild::messageIdGlobalStatisticData) { messageIdGlobalStatisticData = message.id.value.toString() }
}

suspend fun createMessageSimple(channelText: TextChannel, sqlData: SQLData) {
    val message = channelText.createMessage("Initial Message Simple")
    channelText.getMessage(message.id).edit { editMessageSimpleContent(sqlData,this) }
    sqlData.guildSQL.update (TableGuild::messageId) { messageId = message.id.value.toString() }
}

suspend fun createMessageAramMMRData(channelText: TextChannel, sqlData: SQLData, kordLol: List<TableKORD_LOL>) {
    val message = channelText.createMessage("Initial Message AramMMR")
    channelText.getMessage(message.id).edit { editMessageAramMMRDataContent(this, sqlData, kordLol) }
    sqlData.guildSQL.update (TableGuild::messageIdArammmr) { messageIdArammmr = message.id.value.toString() }
}

fun editMessageSimpleContent(sqlData: SQLData, builder: UserMessageModifyBuilder) {
    builder.content = "Статистика по Серверу: ${TimeStamp.now()}\n" +
            "Игр на сервере: ${tableMatch.count { TableMatch::guild eq sqlData.guildSQL }}\n" +
            "Пользователей в базе: ${sqlData.getLOL().size}\n" +
            "Игроков в базе: ${tableLOLPerson.size}\n" +
            "Версия игры: ${LeagueMainObject.LOL_VERSION}\n" +
            "Количество чемпионов: ${LeagueMainObject.LOL_HEROES}"

    printLog(sqlData.guild, "[editMessageSimpleContent] completed")
}

fun editMessageGlobalStatisticContent(builder: UserMessageModifyBuilder, sqlData: SQLData, kordLol: List<TableKORD_LOL>, savedParts: List<TableParticipant>) {

    val dataList = ArrayList<TableParticipantData>()
    savedParts.forEach {part ->
        val firePartData = TableParticipantData()
        val findedObj = dataList.find { it.part?.LOLperson?.LOL_puuid == part.LOLperson?.LOL_puuid }
        if (findedObj == null) {
            firePartData.part = part
            if (firePartData.part!!.win)
                firePartData.statWins++
            firePartData.statGames++
            dataList.add(firePartData)
        } else {
            findedObj.part!!.kills += part.kills
            findedObj.part!!.kills2 += part.kills2
            findedObj.part!!.kills3 += part.kills3
            findedObj.part!!.kills4 += part.kills4
            findedObj.part!!.kills5 += part.kills5
            findedObj.part!!.skillsCast += part.skillsCast
            findedObj.part!!.totalDmgToChampions += part.totalDmgToChampions
            if (part.win)
                findedObj.statWins++
            findedObj.statGames++
        }
    }

    dataList.sortBy { sqlData.getKORDLOLfromParticipant(kordLol, it.part).id }
    val charStr = " / "

//    val list1 = dataList.map { obj -> formatInt(sqlData.getKORDLOLfromParticipant(kordLol, obj.part).id, 2) + "|" + sqlData.getKORDLOLfromParticipant(kordLol, obj.part).asUser(sqlData.guild).lowDescriptor() + "/" + mapWins[sqlData.getKORDLOLfromParticipant(kordLol, obj.part)] }
    val listGames = dataList.map { formatInt(sqlData.getKORDLOLfromParticipant(kordLol, it.part).id, 2) + "| " + formatInt(it.statGames, 3) + charStr + formatInt(it.statWins, 3) + charStr + formatInt(((it.statWins.toDouble() / it.statGames.toDouble()) * 100).toInt(), 2) + "%" }
    val listAllKills = dataList.map {  it.part!!.kills.toFormatK() + charStr + formatInt(it.part!!.kills3, 2) + charStr + formatInt(it.part!!.kills4, 2) + charStr + formatInt(it.part!!.kills5, 2) }

    dataList.forEach {
        it.clearData()
    }

    builder.content = "Статистика Общая\nОбновлено: ${TimeStamp.now()}\n"
    builder.embed {
//        field {
//            name = "User/WinStreak"
//            value = list1.joinToString(separator = "\n")
//            inline = true
//        }
        field {
            name = "Game/Win/WinRate"
            value = listGames.joinToString(separator = "\n")
            inline = true
        }
        field {
            name = "Kill/Triple/Quadra/Penta"
            value = listAllKills.joinToString(separator = "\n")
            inline = true
        }
    }

    printLog(sqlData.guild, "[editMessageGlobalStatisticContent] completed")
}

fun editMessagePentaDataContent(builder: UserMessageModifyBuilder, sqlData: SQLData, kordLol: List<TableKORD_LOL>, savedParts: List<TableParticipant>) {
    val dataList = ArrayList<DataBasic>()

    savedParts.forEach {part ->
        val currentPart = sqlData.getKORDLOLfromParticipant(kordLol, part)
        if (!part.match!!.bots && part.kills5 > 0 && dataList.size < 20) {
            val addedText = if (part.kills5 == 1) "" else "(${part.kills5})"
            dataList.add(
                DataBasic(
                    user = currentPart,
                    text = "Пентакилл$addedText за '${LeagueMainObject.findHeroForKey(part.championId.toString()).toMaxSymbols(10, "..")}'",
                    date = part.match!!.matchDate,
                    match = part.match!!
                )
            )
        }
    }

    dataList.sortByDescending { it.date }

//    val list1 = dataList.map { formatInt(it.user?.id ?: -1, 2) + "|" + it.user?.asUser(sqlData.guild)?.lowDescriptor() }
    val list2 = dataList.map { formatInt(it.user?.id ?: -1, 2) + "| " + it.text }
    val list3 = dataList.map { it.date.toFormatDate() }

    builder.content = "Статистика Пентакиллов (топ 20)\nОбновлено: ${TimeStamp.now()}\n"
    builder.embed {
//        field {
//            name = "Призыватель"
//            value = list1.joinToString(separator = "\n")
//            inline = true
//        }
        field {
            name = "Information"
            value = list2.joinToString(separator = "\n")
            inline = true
        }
        field {
            name = "Date"
            value = list3.joinToString(separator = "\n")
            inline = true
        }
    }

    printLog(sqlData.guild, "[editMessagePentaDataContent] completed")
}

fun editMessageMainDataContent(builder: UserMessageModifyBuilder, sqlData: SQLData, kordLol: List<TableKORD_LOL>, mapWins: HashMap<TableKORD_LOL, Int>) {
    val dataList = ArrayList<TableKORD_LOL>()
    dataList.addAll(kordLol)

    dataList.sortBy { it.id }
    val charStr = " / "

    val list1 = dataList.map { formatInt(it.id, 2) + charStr + it.asUser(sqlData.guild).lowDescriptor() }
    val list2 = dataList.map { it.LOLperson?.LOL_summonerName + "#" + it.LOLperson?.LOL_riotIdTagline }
    val list3 = dataList.map { mapWins[it] }

    builder.content = "Статистика Главная\nОбновлено: ${TimeStamp.now()}\n"
    builder.embed {
        field {
            name = "ID/User"
            value = list1.joinToString(separator = "\n")
            inline = true
        }
        field {
            name = "Nickname"
            value = list2.joinToString(separator = "\n")
            inline = true
        }
        field {
            name = "WinStreak"
            value = list3.joinToString(separator = "\n")
            inline = true
        }
    }

    printLog(sqlData.guild, "[editMessageMainDataContent] completed")
}

fun editMessageAramMMRDataContent(builder: UserMessageModifyBuilder, sqlData: SQLData, kordLol: List<TableKORD_LOL>) {

    data class kordTemp(var kordLOL: TableKORD_LOL, var lastPart: TableParticipant?)

    val dataList = ArrayList<kordTemp>()
    kordLol.forEach {
        dataList.add(kordTemp(it,
            tableParticipant.selectAll().where { TableParticipant::LOLperson eq it.LOLperson }.where { TableMatch::matchMode eq "ARAM" }.orderByDescending(TableParticipant::match).limit(1).getEntity()))
    }

    dataList.sortBy { it.kordLOL.id }
    val charStr = " / "

    val list1 = dataList.map { formatInt(it.kordLOL.id, 2) + "| " + EnumMMRRank.getMMRRank(it.kordLOL.mmrAram).nameRank }
    val list2 = dataList.map { it.kordLOL.mmrAram.toString() + charStr + it.kordLOL.mmrAramSaved }
    val list3 = dataList.map { LeagueMainObject.catchHeroForId(it.lastPart?.championId.toString())?.name + charStr + it.lastPart?.mmr }

    builder.content = "Статистика ММР\nОбновлено: ${TimeStamp.now()}\n"
    builder.embed {
        field {
            name = "ARAM Rank"
            value = list1.joinToString(separator = "\n")
            inline = true
        }
        field {
            name = "MMR/Bonus"
            value = list2.joinToString(separator = "\n")
            inline = true
        }
        field {
            name = "LastGame/MMR"
            value = list3.joinToString(separator = "\n")
            inline = true
        }
    }

    printLog(sqlData.guild, "[editMessageAramMMRDataContent] completed")
}