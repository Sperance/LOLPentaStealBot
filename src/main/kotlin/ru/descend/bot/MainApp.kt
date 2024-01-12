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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import me.jakejmattson.discordkt.dsl.CommandException
import me.jakejmattson.discordkt.dsl.ListenerException
import me.jakejmattson.discordkt.dsl.bot
import me.jakejmattson.discordkt.extensions.TimeStamp
import ru.descend.bot.lolapi.LeagueMainObject
import ru.descend.bot.lolapi.leaguedata.championMasteryDto.ChampionMasteryDto
import ru.descend.bot.postgre.EnumMessageType
import ru.descend.bot.postgre.TableGuild
import ru.descend.bot.postgre.TableKORDPerson
import ru.descend.bot.postgre.TableKORD_LOL
import ru.descend.bot.postgre.TableMatch
import ru.descend.bot.postgre.TableParticipantData
import ru.descend.bot.postgre.PostgreSQL.getGuild
import ru.descend.bot.postgre.Postgre
import ru.descend.bot.postgre.TableLOLPerson
import ru.descend.bot.postgre.TableMessage
import ru.descend.bot.postgre.TableParticipant
import ru.descend.bot.postgre.tableKORDLOL
import ru.descend.bot.postgre.tableKORDPerson
import ru.descend.bot.postgre.tableLOLPerson
import ru.descend.bot.postgre.tableMatch
import ru.descend.bot.postgre.tableMessage
import ru.descend.bot.postgre.tableParticipant
import ru.descend.bot.savedObj.DataBasic
import save
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
            if (exception is IllegalArgumentException)
                return@onException

            when (this) {
                is CommandException -> println("Exception '${exception::class.simpleName}' in command ${event.command?.name}")
                is ListenerException -> println("Exception '${exception::class.simpleName}' in listener ${event::class.simpleName}")
            }
        }
        presence {
            this.status = PresenceStatus.Online
            playing("ARAM")
        }
        onStart {
            printLog("Bot ${this.properties.bot.name} started")
            println("Guilds: ")

            Postgre.initializePostgreSQL()

            kord.guilds.toList().forEach {
                println("\t  ${it.name} [${it.id.value}]")

                isWorkMainThread[it] = true
                sqlCurrentUsers[it.id.value.toString()] = ArrayList()
                sqlCurrentMatches[it.id.value.toString()] = ArrayList()
                sqlCurrentKORD[it.id.value.toString()] = ArrayList()
                sqlCurrentLOL[it.id.value.toString()] = ArrayList()
                sqlCurrentKORDLOL[it.id.value.toString()] = ArrayList()
                sqlCurrentMessages[it.id.value.toString()] = ArrayList()
                val guildSQL = getGuild(it)
                removeMessage(it, guildSQL)

                launch {
                    while (true) {
                        showLeagueHistory(it, guildSQL)
                        delay((10).minutes)
                        globalLOLRequests = 0
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
            printLog(guild, "Delete message: ${it.id}")
            val msgId = it.id.value.toString()
            if (msgId == guildSQL.messageId || msgId == guildSQL.messageIdPentaData || msgId == guildSQL.messageIdGlobalStatisticData || msgId == guildSQL.messageIdMasteryData) {
                Unit
            } else {
                it.delete()
            }
        }
    }
}

var globalLOLRequests = 0
var statusLOLRequests = 0

val sqlCurrentUsers  = HashMap<String, ArrayList<TableKORD_LOL>>()
val sqlCurrentMatches  = HashMap<String, ArrayList<TableMatch>>()
val sqlCurrentKORD  = HashMap<String, ArrayList<TableKORDPerson>>()
val sqlCurrentLOL  = HashMap<String, ArrayList<TableLOLPerson>>()
val sqlCurrentKORDLOL  = HashMap<String, ArrayList<TableKORD_LOL>>()
val sqlCurrentMessages  = HashMap<String, ArrayList<TableMessage>>()
var sqlAllParticipants = ArrayList<TableParticipant>()

var isWorkMainThread = HashMap<Guild, Boolean>()

//suspend fun showGuildStatMessage(guild: Guild, guildData: TableGuild?) {
//    if (guildData == null) {
//        printLog(guild, "Guild not found in SQL")
//        return
//    }
//
//    if (guildData.messageIdStatus.isNotEmpty()) {
//        sqlCurrentMessages[guild.id.value.toString()]!!.forEach {
//            it.sendMessage(guild)
//        }
//    }
//}
//
//suspend fun showMasteryHistory(guild: Guild, guildData: TableGuild?) {
//    printLog("111")
//    if (isWorkMainThread[guild]!! == false) {
//        printLog(guild, "[MainApp] isWorkMainThread false")
//        return
//    }
//    printLog("222")
//    if (guildData == null) {
//        printLog(guild, "Guild not found in SQL")
//        return
//    }
//
//    printLog("333")
//    if (guildData.botChannelId.isNotEmpty()) {
//        printLog("444")
//        val mapUses = HashMap<TableKORD_LOL, ChampionMasteryDto>()
//        printLog("1mapUses: ${mapUses.size}")
//
//        sqlCurrentKORDLOL[guild.id.value.toString()]!!.forEach {
//            if (it.LOLperson == null) return@forEach
//            if (it.LOLperson?.LOL_puuid == "") return@forEach
//            LeagueMainObject.catchChampionMastery(it.LOLperson!!.LOL_puuid)?.let { mastery ->
//                mapUses[it] = mastery
//            }
//        }
//        printLog("2mapUses: ${mapUses.size}")
//        printLog("3mapUses: $mapUses")
//        printLog("555")
//        val channelText = guild.getChannelOf<TextChannel>(Snowflake(guildData.botChannelId))
//        //Таблица по очкам чемпионам
//        launch {
//            printLog("666")
//            editMessageGlobal(channelText, guildData.messageIdMasteryData, {
//                editMessageMasteryContent(it, mapUses, guild)
//            }) {
//                createMessageMastery(channelText, mapUses, guildData)
//            }
//            mapUses.clear()
//        }.join()
//    }
//}

suspend fun showLeagueHistory(guild: Guild, guildData: TableGuild?) {

    if (isWorkMainThread[guild]!! == false) {
        printLog(guild, "[MainApp] isWorkMainThread false")
        return
    }

    if (guildData == null) {
        printLog(guild, "Guild not found in SQL")
        return
    }

    if (guildData.botChannelId.isNotEmpty()) {

        sqlAllParticipants.clear()
        sqlAllParticipants.addAll(tableParticipant.getAll { TableParticipant::id greaterEq 0 })
        sqlAllParticipants.sortByDescending { par -> par.match!!.matchDate }

        if (sqlCurrentUsers[guild.id.value.toString()]!!.isEmpty()) {
            sqlCurrentUsers[guild.id.value.toString()]!!.addAll(tableKORDLOL.getAll { TableKORD_LOL::guild eq guildData })
            val sizeC = sqlCurrentUsers[guild.id.value.toString()]!!.size
            if (sizeC > 0) printLog(guild, "initalize sql KORD users size: $sizeC")
        }
        if (sqlCurrentMatches[guild.id.value.toString()]!!.isEmpty()) {
            sqlCurrentMatches[guild.id.value.toString()]!!.addAll(tableMatch.getAll { TableMatch::guild eq guildData })
            val sizeC = sqlCurrentMatches[guild.id.value.toString()]!!.size
            if (sizeC > 0) printLog(guild, "initalize sql matching size: $sizeC")
        }

        sqlCurrentMessages[guild.id.value.toString()]!!.addAll(tableMessage.getAll { TableMessage::guild eq guildData })
        sqlCurrentKORDLOL[guild.id.value.toString()]!!.addAll(tableKORDLOL.getAll { TableKORD_LOL::guild eq guildData })
        sqlCurrentKORD[guild.id.value.toString()]!!.addAll(tableKORDPerson.getAll { TableKORDPerson::guild eq guildData })
        sqlCurrentKORD[guild.id.value.toString()]!!.forEach {
            sqlCurrentLOL[guild.id.value.toString()]!!.addAll(it.LOLpersons)
        }
        val mapUses = HashMap<TableKORD_LOL, ChampionMasteryDto>()

        launch {
            sqlCurrentKORDLOL[guild.id.value.toString()]!!.forEach {
                if (it.LOLperson == null) return@forEach
                if (it.LOLperson?.LOL_puuid == "") return@forEach
                printLog("\t[REQUESTS STARTED] FOR: ${it.LOLperson?.LOL_summonerName} ${it.LOLperson?.LOL_puuid}")
                LeagueMainObject.catchMatchID(it.LOLperson!!.LOL_puuid, 0,3).forEach ff@ { matchId ->
                    if (sqlCurrentMatches[guild.id.value.toString()]!!.find { mch -> mch.matchId == matchId } == null) {
                        LeagueMainObject.catchMatch(matchId)?.let { match ->
                            sqlCurrentMatches[guild.id.value.toString()]!!.add(guildData.addMatch(match))
                        }
                    }
                }
//                LeagueMainObject.catchChampionMastery(it.LOLperson!!.LOL_puuid)?.let { mastery ->
//                    mapUses[it] = mastery
//                }
                printLog("\t[REQUESTS ENDED] FOR: ${it.LOLperson?.LOL_summonerName} ${it.LOLperson?.LOL_puuid}")
            }
        }.join()

        val winStreakMap = catchWinStreak(sqlCurrentMatches[guild.id.value.toString()]!!, guild)
        val channelText = guild.getChannelOf<TextChannel>(Snowflake(guildData.botChannelId))

        //Таблица Пентакиллов
        launch {
            editMessageGlobal(channelText, guildData.messageIdPentaData, {
                editMessagePentaDataContent(it, guild, guildData)
            }) {
                createMessagePentaData(channelText, guildData)
            }
        }.join()
        //Таблица по играм\винрейту\сериям убийств
        launch {
            editMessageGlobal(channelText, guildData.messageIdGlobalStatisticData, {
                editMessageGlobalStatisticContent(it, sqlCurrentMatches[guild.id.value.toString()]!!, guild, winStreakMap)
            }) {
                createMessageGlobalStatistic(channelText, sqlCurrentMatches[guild.id.value.toString()]!!, guildData, winStreakMap)
            }
        }.join()
        //Таблица по очкам чемпионам
//        launch {
//            editMessageGlobal(channelText, guildData.messageIdMasteryData, {
//                editMessageMasteryContent(it, mapUses, guild)
//            }) {
//                createMessageMastery(channelText, mapUses, guildData)
//            }
//            mapUses.clear()
//        }.join()
        //Общая статистика по серверу
        launch {
            editMessageGlobal(channelText, guildData.messageId, {
                editMessageSimpleContent(guild, it)
            }) {
                createMessageSimple(channelText, guildData)
            }
        }.join()
    }
}

fun catchWinStreak(allMatches: ArrayList<TableMatch>, guild: Guild): HashMap<TableKORD_LOL, Int> {

    val mapStreak = HashMap<TableKORD_LOL, ArrayList<TableMatch>>()
    val mapResult = HashMap<TableKORD_LOL, Int>()

    //Инициализация
    sqlCurrentKORDLOL[guild.id.value.toString()]!!.forEach {
        mapStreak[it] = ArrayList()
        mapResult[it] = 0
    }

    //Заполнение парами Игрок-Матчи
    allMatches.sortBy { it.matchDate }
    allMatches.forEach {match ->
        sqlAllParticipants.filter { part -> part.match?.matchId == match.matchId }.forEach {part ->
            if (mapStreak.containsKey(sqlCurrentKORDLOL[guild.id.value.toString()]!!.find { it.LOLperson == part.LOLperson }))
                mapStreak[sqlCurrentKORDLOL[guild.id.value.toString()]!!.find { it.LOLperson == part.LOLperson }]!!.add(match)
        }
    }

    //Сортировка мапа Игрок-Матчи по самому последнему его матчу
    mapStreak.forEach { (firePerson, _) ->
        mapStreak[firePerson]!!.sortBy { it.matchDate }

        //Учитываем для оптимизации только 20 последних игр
        while (mapStreak[firePerson]!!.size > 20) {
            mapStreak[firePerson]!!.removeFirst()
        }
    }

    mapStreak.forEach { (firePerson, fireMatches) ->
        var counter = 0
        fireMatches.forEach { match ->
            val objectPerson = sqlAllParticipants.find { it.match?.matchId == match.matchId && it.LOLperson?.LOL_puuid == firePerson.LOLperson?.LOL_puuid }
            if (objectPerson != null) {
                if (objectPerson.win) {
                    if (counter < 0) counter = 0
                    counter++
                } else {
                    if (counter > 0) counter = 0
                    counter--
                }
            }
        }
        mapResult[firePerson] = counter
    }

    return mapResult
}

suspend fun editMessageGlobal(channelText: TextChannel, messageId: String, editBody: (UserMessageModifyBuilder) -> Unit, createBody: suspend () -> Unit) {
    if (messageId.isBlank()) {
        createBody.invoke()
    } else {
        val message = channelText.getMessageOrNull(Snowflake(messageId))
        if (message != null) {
            message.edit { editBody.invoke(this) }
        } else {
            createBody.invoke()
        }
    }
}

suspend fun createMessagePentaData(channelText: TextChannel, file: TableGuild) {
    val message = channelText.createMessage("Initial Message")
    channelText.getMessage(message.id).edit { editMessagePentaDataContent(this, message.getGuild(), file) }
    file.update { messageIdPentaData = message.id.value.toString() }
}

suspend fun createMessageGlobalStatistic(channelText: TextChannel, allMatches: ArrayList<TableMatch>, file: TableGuild, mapWins: HashMap<TableKORD_LOL, Int>) {
    val message = channelText.createMessage("Initial Message")
    channelText.getMessage(message.id).edit {
        editMessageGlobalStatisticContent(this, allMatches, message.getGuild(), mapWins)
    }
    file.update { messageIdGlobalStatisticData = message.id.value.toString() }
}

suspend fun createMessageSimple(channelText: TextChannel, file: TableGuild) {
    val message = channelText.createMessage("Initial Message")
    channelText.getMessage(message.id).edit { editMessageSimpleContent(channelText.getGuild(),this) }
    file.update { messageId = message.id.value.toString() }
}

suspend fun createMessageMastery(channelText: TextChannel, map: HashMap<TableKORD_LOL, ChampionMasteryDto>, file: TableGuild) {
    val message = channelText.createMessage("Initial Message")
    channelText.getMessage(message.id).edit { editMessageMasteryContent(this, map, message.getGuild()) }
    file.update { messageIdMasteryData = message.id.value.toString() }
}

fun editMessageMasteryContent(builder: UserMessageModifyBuilder, map: HashMap<TableKORD_LOL, ChampionMasteryDto>, guild: Guild) {

    val sortedMap = map.toSortedMap { p0, p1 -> p0.id.compareTo(p1.id) }

    val list1 = sortedMap.map { obj -> formatInt(sqlCurrentKORDLOL[guild.id.value.toString()]!!.find { it.LOLperson == obj.key.LOLperson }!!.id, 2) + "|" + sqlCurrentKORDLOL[guild.id.value.toString()]!!.find { it.LOLperson == obj.key.LOLperson }!!.asUser(guild).lowDescriptor() }
    val listHeroes = sortedMap.map { formatInt(sqlCurrentKORDLOL[guild.id.value.toString()]!!.find { per -> per.LOLperson == it.key.LOLperson }!!.id, 2) + "| " + LeagueMainObject.findHeroForKey(it.value.getOrNull(0)?.championId.toString()) + " / " + LeagueMainObject.findHeroForKey(it.value.getOrNull(1)?.championId.toString()) + " / " + LeagueMainObject.findHeroForKey(it.value.getOrNull(2)?.championId.toString()) }
    val listPoints = sortedMap.map { formatInt(sqlCurrentKORDLOL[guild.id.value.toString()]!!.find { per -> per.LOLperson == it.key.LOLperson }!!.id, 2) + "| " + it.value.getOrNull(0)?.championPoints?.toFormatK() + " / " + it.value.getOrNull(1)?.championPoints?.toFormatK() + " / " + it.value.getOrNull(2)?.championPoints?.toFormatK() }

    builder.content = "Статистика по Чемпионам\n"
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

fun editMessageSimpleContent(guild: Guild, builder: UserMessageModifyBuilder) {
    builder.content = "Статистика по Серверу: ${TimeStamp.now()}\n" +
            "Игр на сервере: ${sqlCurrentMatches[guild.id.value.toString()]!!.size}\n" +
            "Игроков в базе: ${tableLOLPerson.size}\n" +
            "Пользователей в базе: ${tableKORDPerson.count { TableKORDPerson::guild eq getGuild(guild) }}\n" +
            "Версия игры: ${LeagueMainObject.LOL_VERSION}\n" +
            "Количество чемпионов: ${LeagueMainObject.LOL_HEROES}"
}

fun editMessageGlobalStatisticContent(builder: UserMessageModifyBuilder, allMatches: ArrayList<TableMatch>, guild: Guild, mapWins: HashMap<TableKORD_LOL, Int>) {

    val dataList = ArrayList<TableParticipantData>()

    allMatches.forEach {match ->
        sqlAllParticipants.filter { part -> part.match?.matchId == match.matchId && sqlCurrentLOL[guild.id.value.toString()]!!.find { it.LOL_puuid == part.LOLperson?.LOL_puuid } != null }.forEach {part ->
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
    }

    dataList.sortBy { it.part?.LOLperson?.id }
    val charStr = " / "

    val list1 = dataList.map { obj -> formatInt(sqlCurrentKORDLOL[guild.id.value.toString()]!!.find { it.LOLperson == obj.part!!.LOLperson }!!.id, 2) + "|" + sqlCurrentKORDLOL[guild.id.value.toString()]!!.find { it.LOLperson == obj.part!!.LOLperson }!!.asUser(guild).lowDescriptor() + ":" + mapWins[sqlCurrentKORDLOL[guild.id.value.toString()]!!.find { it.LOLperson == obj.part!!.LOLperson }!!] }
    val listGames = dataList.map { formatInt(sqlCurrentKORDLOL[guild.id.value.toString()]!!.find { per -> per.LOLperson == it.part!!.LOLperson }!!.id, 2) + "| " + formatInt(it.statGames, 3) + charStr + formatInt(it.statWins, 3) + charStr + formatInt(((it.statWins.toDouble() / it.statGames.toDouble()) * 100).toInt(), 2) + "%" }
    val listAllKills = dataList.map { formatInt(sqlCurrentKORDLOL[guild.id.value.toString()]!!.find { per -> per.LOLperson == it.part!!.LOLperson }!!.id, 2) + "| " + it.part!!.kills.toFormatK() + charStr + formatInt(it.part!!.kills3, 2) + charStr + formatInt(it.part!!.kills4, 2) + charStr + formatInt(it.part!!.kills5, 2) }

    dataList.forEach {
        it.clearData()
    }

    builder.content = "Статистика Общая\n"
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
}

fun editMessagePentaDataContent(builder: UserMessageModifyBuilder, guild: Guild, tableGuild: TableGuild) {
    val dataList = ArrayList<DataBasic>()

    sqlAllParticipants.forEach {part ->
        if (sqlCurrentKORDLOL[guild.id.value.toString()]!!.find { part.LOLperson == it.LOLperson } != null){
            if (part.kills5 > 0 && dataList.size < 20) dataList.add(
                DataBasic(user = sqlCurrentKORDLOL[guild.id.value.toString()]!!.find { it.LOLperson == part.LOLperson }, text = "Пентакилл за '${LeagueMainObject.findHeroForKey(part.championId.toString())}'", date = part.match!!.matchDate, match = part.match!!)
            )
        }
    }

    dataList.sortByDescending { it.date }
//    dataList.forEach {
//        val textMessage = "Поздравляем!!!\n\nПризыватель ${it.user?.asUser(guild)?.lowDescriptor()} сделал Пентакилл за чемпиона ${it.text.replace("Пентакилл за ", "")}.\nВыражаем поздравления от всего сервера!!! "
//        val tMessage = TableMessage(message = textMessage, type = EnumMessageType.PENTA, guild = tableGuild, addKey = it.match.matchId, KORD_LOL = it.user)
//        if (sqlCurrentMessages[guild.id.value.toString()]!!.find { curMsg -> curMsg.messageInnerId == tMessage.messageInnerId } == null)
//            tMessage.save()
//    }

    val list1 = dataList.map { formatInt(it.user?.id ?: -1, 2) + "|" + it.user?.asUser(guild)?.lowDescriptor() }
    val list2 = dataList.map { formatInt(it.user?.id ?: -1, 2) + "| " + it.text }
    val list3 = dataList.map { formatInt(it.user?.id ?: -1, 2) + "| " + it.date.toFormatDate() }

    builder.content = "Статистика Пентакиллов\n"
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
}