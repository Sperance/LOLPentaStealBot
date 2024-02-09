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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toList
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
import ru.descend.bot.postgre.tables.tableLOLPerson
import ru.descend.bot.postgre.tables.tableMatch
import ru.descend.bot.savedObj.DataBasic
import update
import java.awt.Color
import kotlin.time.Duration.Companion.minutes

@OptIn(PrivilegedIntent::class)
@KordPreview
fun main() {
    println("Initializing is Started")
    LeagueMainObject.catchHeroNames()
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
            printLog("Bot ${this.properties.bot.name} started")
            println("Guilds: ")

            Postgre.initializePostgreSQL()
            var tickServerMin = 0

            kord.guilds.toList().forEach {
                println("\t  ${it.name} [${it.id.value}]")

                isWorkMainThread[it] = true

                val guildSQL = getGuild(it)
                removeMessage(it, guildSQL)

                val data = SQLData(it, guildSQL)
                mainMapData[it] = data

                launch {
                    while (true) {

//                        if (tickServerMin % 2 == 0) {
                            globalLOLRequests = 0
//                        }

                        if (data.guildSQL.botChannelId.isNotEmpty()) {
//                            if (tickServerMin % 9 == 0) {
//                                launch {
                                    printLog(it, "[START] showLeagueHistory")
                                    showLeagueHistory(it)
                                    printLog(it, "[END] showLeagueHistory")
//                                }
//                            }

//                            if (tickServerMin != 0 && tickServerMin % 60 == 0) {
//                                launch {
//                                    printLog(it, "[START] showMasteryHistory")
//                                    showMasteryHistory(it)
//                                    printLog(it, "[END] showMasteryHistory")
//                                }
//                            }
                        }

                        delay((5).minutes)
//                        tickServerMin++
//                        if (tickServerMin > 60) tickServerMin = 5
                    }
                }
            }
        }
    }
}

suspend fun removeMessage(guild: Guild, guildSQL: TableGuild) {
    if (guildSQL.botChannelId.isNotEmpty()) {
        val channelText = guild.getChannelOf<TextChannel>(Snowflake(guildSQL.botChannelId))
        channelText.messages.collect {
            val msgId = it.id.value.toString()
            if (msgId == guildSQL.messageId || msgId == guildSQL.messageIdPentaData || msgId == guildSQL.messageIdGlobalStatisticData || msgId == guildSQL.messageIdMasteryData) {
                Unit
            } else {
                it.delete()
            }
        }
    }
}

lateinit var globalChampionsDTO: ChampionsDTO
var globalLOLRequests = 0
var statusLOLRequests = 0

var isWorkMainThread = HashMap<Guild, Boolean>()
var mainMapData = HashMap<Guild, SQLData>()

suspend fun showMasteryHistory(guild: Guild) {

    val sqlData = mainMapData[guild]

    if (sqlData == null) {
        printLog(guild, "[MainApp::showMasteryHistory] sqlData null")
        return
    }

    if (isWorkMainThread[sqlData.guild]!! == false) {
        printLog(sqlData.guild, "[MainApp] isWorkMainThread false")
        return
    }

    if (!sqlData.isNeedUpdateMastery) {
        printLog(sqlData.guild, "[MainApp::showMasteryHistory] not need update")
        return
    } else {
        sqlData.isNeedUpdateMastery = false
    }

    val mapUses = HashMap<TableKORD_LOL, ChampionMasteryDto>()

    sqlData.getKORDLOL().forEach {
        if (it.LOLperson == null) return@forEach
        if (it.LOLperson?.LOL_puuid == "") return@forEach
        LeagueMainObject.catchChampionMastery(it.LOLperson!!.LOL_puuid)?.let { mastery ->
            mapUses[it] = mastery
        }
    }

    val channelText = sqlData.guild.getChannelOf<TextChannel>(Snowflake(sqlData.guildSQL.botChannelId))
    //Таблица по очкам чемпионам
    launch {
        editMessageGlobal(channelText, sqlData.guildSQL.messageIdMasteryData, {
            editMessageMasteryContent(it, mapUses, sqlData.guild)
        }) {
            createMessageMastery(channelText, mapUses, sqlData.guildSQL)
        }
        mapUses.clear()
    }.join()
}

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

    launch {
        val kordlol = sqlData.getKORDLOL()
        val tableMMR = sqlData.getMMR()
        val currentUsers = ArrayList<String?>()
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

            LeagueMainObject.catchMatchID(it.LOLperson!!.LOL_puuid, 0,50).forEach ff@ { matchId ->
                if (!checkMatches.contains(matchId)) checkMatches.add(matchId)
            }
            sqlData.getNewMatches(checkMatches).forEach {newMatch ->
                LeagueMainObject.catchMatch(newMatch)?.let { match ->
                    match.info.participants.forEach { part ->
                        if (currentUsers.contains(part.puuid)) {
                            currentUsers.remove(part.puuid)
                        }
                    }
                    sqlData.isNeedUpdateMastery = true
                    sqlData.addMatch(match, kordlol, tableMMR)
                }
            }
        }
    }.join()

    val winStreakMap = catchWinStreak(sqlData)
    val channelText = sqlData.guild.getChannelOf<TextChannel>(Snowflake(sqlData.guildSQL.botChannelId))
    //Таблица Пентакиллов
    launch {
        editMessageGlobal(channelText, sqlData.guildSQL.messageIdPentaData, {
            editMessagePentaDataContent(it, sqlData)
        }) {
            createMessagePentaData(channelText, sqlData)
        }
    }.join()
    //Таблица по играм\винрейту\сериям убийств
    launch {
        editMessageGlobal(channelText, sqlData.guildSQL.messageIdGlobalStatisticData, {
            editMessageGlobalStatisticContent(it, sqlData, winStreakMap)
        }) {
            createMessageGlobalStatistic(channelText, sqlData, winStreakMap)
        }
    }.join()
    //Общая статистика по серверу
    launch {
        editMessageGlobal(channelText, sqlData.guildSQL.messageId, {
            editMessageSimpleContent(sqlData, it)
        }) {
            createMessageSimple(channelText, sqlData)
        }
    }.join()
}

fun catchWinStreak(sqlData: SQLData): HashMap<TableKORD_LOL, Int> {

    val mapResult = HashMap<TableKORD_LOL, Int>()

    //Инициализация
    sqlData.getKORDLOL().forEach {
        var counter = 0
        sqlData.getLastParticipants(it.LOLperson?.LOL_puuid, 20).forEach {objectPerson ->
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

suspend fun createMessagePentaData(channelText: TextChannel, sqlData: SQLData) {
    val message = channelText.createMessage("Initial Message PentaData")
    channelText.getMessage(message.id).edit { editMessagePentaDataContent(this, sqlData) }
    sqlData.guildSQL.update (TableGuild::messageIdPentaData) { messageIdPentaData = message.id.value.toString() }
}

suspend fun createMessageGlobalStatistic(channelText: TextChannel, sqlData: SQLData, mapWins: HashMap<TableKORD_LOL, Int>) {
    val message = channelText.createMessage("Initial Message GlobalStatistic")
    channelText.getMessage(message.id).edit {
        editMessageGlobalStatisticContent(this, sqlData, mapWins)
    }
    sqlData.guildSQL.update (TableGuild::messageIdGlobalStatisticData) { messageIdGlobalStatisticData = message.id.value.toString() }
}

suspend fun createMessageSimple(channelText: TextChannel, sqlData: SQLData) {
    val message = channelText.createMessage("Initial Message Simple")
    channelText.getMessage(message.id).edit { editMessageSimpleContent(sqlData,this) }
    sqlData.guildSQL.update (TableGuild::messageId) { messageId = message.id.value.toString() }
}

suspend fun createMessageMastery(channelText: TextChannel, map: HashMap<TableKORD_LOL, ChampionMasteryDto>, file: TableGuild) {
    val message = channelText.createMessage("Initial Message Mastery")
    channelText.getMessage(message.id).edit { editMessageMasteryContent(this, map, message.getGuild()) }
    file.update (TableGuild::messageIdMasteryData) { messageIdMasteryData = message.id.value.toString() }
}

fun editMessageMasteryContent(builder: UserMessageModifyBuilder, map: HashMap<TableKORD_LOL, ChampionMasteryDto>, guild: Guild) {

    val sortedMap = map.toSortedMap { p0, p1 -> p0.id.compareTo(p1.id) }

    val list1 = sortedMap.map { obj -> formatInt(obj.key.id, 2) + "|" + obj.key.asUser(guild).lowDescriptor() }
    val listHeroes = sortedMap.map { formatInt(it.key.id, 2) + "| " + LeagueMainObject.findHeroForKey(it.value.getOrNull(0)?.championId.toString()) + " / " + LeagueMainObject.findHeroForKey(it.value.getOrNull(1)?.championId.toString()) + " / " + LeagueMainObject.findHeroForKey(it.value.getOrNull(2)?.championId.toString()) }
    val listPoints = sortedMap.map { formatInt(it.key.id, 2) + "| " + it.value.getOrNull(0)?.championPoints?.toFormatK() + " / " + it.value.getOrNull(1)?.championPoints?.toFormatK() + " / " + it.value.getOrNull(2)?.championPoints?.toFormatK() }

    builder.content = "Статистика по Чемпионам\nОбновлено: ${TimeStamp.now()}\n"
    builder.embed {
        field {
            name = "User"
            value = list1.joinToString(separator = "\n")
            inline = true
        }
        field {
            name = "Heroes"
            value = listHeroes.joinToString(separator = "\n")
            inline = true
        }
        field {
            name = "Points"
            value = listPoints.joinToString(separator = "\n")
            inline = true
        }
    }
}

fun editMessageSimpleContent(sqlData: SQLData, builder: UserMessageModifyBuilder) {
    builder.content = "Статистика по Серверу: ${TimeStamp.now()}\n" +
            "Игр на сервере: ${tableMatch.count { TableMatch::guild eq sqlData.guildSQL }}\n" +
            "Пользователей в базе: ${sqlData.getLOL().size}\n" +
            "Игроков в базе: ${tableLOLPerson.size}\n" +
            "Версия игры: ${LeagueMainObject.LOL_VERSION}\n" +
            "Количество чемпионов: ${LeagueMainObject.LOL_HEROES}"
}

fun editMessageGlobalStatisticContent(builder: UserMessageModifyBuilder, sqlData: SQLData, mapWins: HashMap<TableKORD_LOL, Int>) {

    val dataList = ArrayList<TableParticipantData>()
    sqlData.getSavedParticipants().forEach {part ->
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

    dataList.sortBy { it.part?.LOLperson?.id }
    val charStr = " / "

    val kordlol = sqlData.getKORDLOL()

    val list1 = dataList.map { obj -> formatInt(sqlData.getKORDLOLfromParticipant(kordlol, obj.part).id, 2) + "|" + sqlData.getKORDLOLfromParticipant(kordlol, obj.part).asUser(sqlData.guild).lowDescriptor() + ":" + mapWins[sqlData.getKORDLOLfromParticipant(kordlol, obj.part)] }
    val listGames = dataList.map { formatInt(sqlData.getKORDLOLfromParticipant(kordlol, it.part).id, 2) + "| " + formatInt(it.statGames, 3) + charStr + formatInt(it.statWins, 3) + charStr + formatInt(((it.statWins.toDouble() / it.statGames.toDouble()) * 100).toInt(), 2) + "%" }
    val listAllKills = dataList.map { formatInt(sqlData.getKORDLOLfromParticipant(kordlol, it.part).id, 2) + "| " + it.part!!.kills.toFormatK() + charStr + formatInt(it.part!!.kills3, 2) + charStr + formatInt(it.part!!.kills4, 2) + charStr + formatInt(it.part!!.kills5, 2) }

    dataList.forEach {
        it.clearData()
    }

    builder.content = "Статистика Общая\nОбновлено: ${TimeStamp.now()}\n"
    builder.embed {
        field {
            name = "User/WinStreak"
            value = list1.joinToString(separator = "\n")
            inline = true
        }
        field {
            name = "Game/Win/WinRate"
            value = listGames.joinToString(separator = "\n")
            inline = true
        }
        field {
            name = "Kill/Triple/Quadr/Penta"
            value = listAllKills.joinToString(separator = "\n")
            inline = true
        }
    }

    printLog(sqlData.guild, "[editMessageGlobalStatisticContent] completed")
}

fun editMessagePentaDataContent(builder: UserMessageModifyBuilder, sqlData: SQLData) {
    val dataList = ArrayList<DataBasic>()

    val kordlol = sqlData.getKORDLOL()

    sqlData.getSavedParticipants().forEach {part ->
        val currentPart = sqlData.getKORDLOLfromParticipant(kordlol, part)
        if (!part.match!!.bots && part.kills5 > 0 && dataList.size < 20) {
            val addedText = if (part.kills5 == 1) "" else "(${part.kills5})"
            dataList.add(
                DataBasic(
                    user = currentPart,
                    text = "Пентакилл$addedText за '${LeagueMainObject.findHeroForKey(part.championId.toString())}'",
                    date = part.match!!.matchDate,
                    match = part.match!!
                )
            )
        }
    }

    dataList.sortByDescending { it.date }

    val list1 = dataList.map { formatInt(it.user?.id ?: -1, 2) + "|" + it.user?.asUser(sqlData.guild)?.lowDescriptor() }
    val list2 = dataList.map { formatInt(it.user?.id ?: -1, 2) + "| " + it.text }
    val list3 = dataList.map { formatInt(it.user?.id ?: -1, 2) + "| " + it.date.toFormatDate() }

    builder.content = "Статистика Пентакиллов (топ 20)\nОбновлено: ${TimeStamp.now()}\n"
    builder.embed {
        field {
            name = "Призыватель"
            value = list1.joinToString(separator = "\n")
            inline = true
        }
        field {
            name = "Суета"
            value = list2.joinToString(separator = "\n")
            inline = true
        }
        field {
            name = "Дата"
            value = list3.joinToString(separator = "\n")
            inline = true
        }
    }

    printLog(sqlData.guild, "[editMessagePentaDataContent] completed")
}