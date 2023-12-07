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
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import me.jakejmattson.discordkt.dsl.CommandException
import me.jakejmattson.discordkt.dsl.ListenerException
import me.jakejmattson.discordkt.dsl.bot
import me.jakejmattson.discordkt.extensions.TimeStamp
import ru.descend.bot.data.Configuration
import ru.descend.bot.firebase.CompleteResult
import ru.descend.bot.firebase.F_MATCHES
import ru.descend.bot.firebase.F_USERS
import ru.descend.bot.firebase.FireGuild
import ru.descend.bot.firebase.FireMatch
import ru.descend.bot.firebase.FireParticipant
import ru.descend.bot.firebase.FirePerson
import ru.descend.bot.firebase.FirebaseService
import ru.descend.bot.lolapi.LeagueMainObject
import ru.descend.bot.lolapi.leaguedata.championMasteryDto.ChampionMasteryDto
import ru.descend.bot.savedObj.DataBasic
import ru.descend.bot.savedObj.getStrongDate
import java.awt.Color
import java.time.Duration

@OptIn(PrivilegedIntent::class)
@KordPreview
fun main() {
    println("Initializing is Started")
    LeagueMainObject.heroNames = LeagueMainObject.catchHeroNames()
    bot(catchToken()[0]) {
        prefix {
            Configuration.prefix
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

            val jobArray = ArrayList<Job>()

            kord.guilds.toList().forEach {
                println("\t  ${it.name} [${it.id.value}]")

                var guildData = FirebaseService.getGuild(it)
                if (guildData == null) {
                    FirebaseService.addGuild(it)
                    guildData = FirebaseService.getGuild(it)
                }

                removeMessage(it)
                arrayCurrentMatches[it.id.value.toString()] = ArrayList()

                jobArray.add(CoroutineScope(Dispatchers.IO).launch {
                    while (true) {
                        showLeagueHistory(it, guildData!!)
                        delay(Duration.ofMinutes(30).toMillis())
                    }
                })
            }

            CoroutineScope(Dispatchers.Default).launch {
                while (true) {
                    delay(Duration.ofMinutes(10).toMillis())
                    jobArray.forEach { job ->
                        if (!job.isActive) {
                            printLog("Job resetted")
                            job.start()
                        }
                    }
                }
            }
        }
    }
}

suspend fun removeMessage(guild: Guild) {
    var guildData = FirebaseService.getGuild(guild)
    if (guildData == null) {
        FirebaseService.addGuild(guild)
        guildData = FirebaseService.getGuild(guild)
    }
    if (guildData!!.botChannelId.isNotEmpty()) {
        printLog("Clean channel start ${guild.id.value}")
        val channelText = guild.getChannelOf<TextChannel>(Snowflake(guildData.botChannelId))
        channelText.messages.collect {
            val msgId = it.id.value.toString()
//            if (msgId == guildData.messageId || msgId == guildData.messageIdPentaData || msgId == guildData.messageIdGlobalStatisticData || msgId == guildData.messageIdMasteryData) {
//
//            } else {
                it.delete()
//            }
        }
        printLog("Clean channel end ${guild.id.value}")
    }
}

private val arrayCurrentMatches = HashMap<String, ArrayList<FireMatch>>()

suspend fun showLeagueHistory(guild: Guild, guildData: FireGuild) {
    val allPersons = FirebaseService.getArrayFromCollection<FirePerson>(FirebaseService.collectionGuild(guild, F_USERS)).await()
    val mapUses = HashMap<FirePerson, ChampionMasteryDto>()
    var newMatch = 0

    if (arrayCurrentMatches[guild.id.value.toString()]!!.isEmpty()) {
        arrayCurrentMatches[guild.id.value.toString()]!!.addAll(FirebaseService.getArrayFromCollection<FireMatch>(FirebaseService.collectionGuild(guild, F_MATCHES)).await())
        printLog("[${guild.id.value}] initalize matching size: " + arrayCurrentMatches[guild.id.value.toString()]!!.size)
    }

    allPersons.forEach {
        if (it.LOL_puuid == "") return@forEach
        LeagueMainObject.catchMatchID(it.LOL_puuid).forEach { matchId ->
            LeagueMainObject.catchMatch(matchId)?.let { match ->
                when (FirebaseService.addMatchToGuild(guild, match)) {
                    is CompleteResult.Error -> null
                    is CompleteResult.Success -> {
                        newMatch++
                        arrayCurrentMatches[guild.id.value.toString()]!!.add(FireMatch(match))
                        printLog("[${guild.id.value}] match ++ Size: " + arrayCurrentMatches[guild.id.value.toString()]!!.size)
                    }
                }
            }
        }
        LeagueMainObject.catchChampionMastery(it.LOL_puuid)?.let {mastery ->
            mapUses[it] = mastery
        }
    }

    val curMatches: ArrayList<FireMatch> = arrayCurrentMatches[guild.id.value.toString()]!!.clone() as ArrayList<FireMatch>

    if (guildData.botChannelId.isNotEmpty()) {
        val channelText = guild.getChannelOf<TextChannel>(Snowflake(guildData.botChannelId))

        editMessageGlobal(channelText, guildData.messageIdPentaData, {
            editMessagePentaDataContent(it, curMatches, allPersons, guild)
        }) {
            createMessagePentaData(channelText, curMatches, allPersons, guildData)
        }
        editMessageGlobal(channelText, guildData.messageIdGlobalStatisticData, {
            editMessageGlobalStatisticContent(it, curMatches, allPersons, guild)
        }) {
            createMessageGlobalStatistic(channelText, curMatches, allPersons, guildData)
        }
        editMessageGlobal(channelText, guildData.messageIdMasteryData, {
            editMessageMasteryContent(it, mapUses, allPersons, guild)
        }) {
            createMessageMastery(channelText, mapUses, allPersons, guildData)
        }
        editMessageGlobal(channelText, guildData.messageId, {
            editMessageSimpleContent(it, curMatches, allPersons)
        }) {
            createMessageSimple(channelText, curMatches, allPersons, guildData)
        }
        mapUses.clear()
    }
}

suspend fun editMessageGlobal(
    channelText: TextChannel,
    messageId: String,
    editBody: (UserMessageModifyBuilder) -> Unit,
    createBody: suspend () -> Unit
) {
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

suspend fun createMessagePentaData(
    channelText: TextChannel,
    allMatches: ArrayList<FireMatch>,
    allPersons: ArrayList<FirePerson>,
    file: FireGuild
) {
    val message = channelText.createMessage("Initial Message")
    channelText.getMessage(message.id).edit { editMessagePentaDataContent(this, allMatches, allPersons, message.getGuild()) }
    file.messageIdPentaData = message.id.value.toString()
    printLog(file.fireSaveData())
}

suspend fun createMessageGlobalStatistic(
    channelText: TextChannel,
    allMatches: ArrayList<FireMatch>,
    allPersons: ArrayList<FirePerson>,
    file: FireGuild
) {
    val message = channelText.createMessage("Initial Message")
    channelText.getMessage(message.id).edit { editMessageGlobalStatisticContent(this, allMatches, allPersons, message.getGuild()) }
    file.messageIdGlobalStatisticData = message.id.value.toString()
    printLog(file.fireSaveData())
}

suspend fun createMessageSimple(
    channelText: TextChannel,
    map: ArrayList<FireMatch>,
    allPersons: ArrayList<FirePerson>,
    file: FireGuild
) {
    val message = channelText.createMessage("Initial Message")
    channelText.getMessage(message.id).edit { editMessageSimpleContent(this, map, allPersons) }
    file.messageId = message.id.value.toString()
    printLog(file.fireSaveData())
}

suspend fun createMessageMastery(
    channelText: TextChannel,
    map: HashMap<FirePerson, ChampionMasteryDto>,
    allPersons: ArrayList<FirePerson>,
    file: FireGuild
) {
    val message = channelText.createMessage("Initial Message")
    channelText.getMessage(message.id).edit { editMessageMasteryContent(this, map, allPersons, message.getGuild()) }
    file.messageIdMasteryData = message.id.value.toString()
    printLog(file.fireSaveData())
}

fun editMessageMasteryContent(
    builder: UserMessageModifyBuilder,
    map: HashMap<FirePerson, ChampionMasteryDto>,
    allPersons: ArrayList<FirePerson>,
    guild: Guild
) {

    val sortedMap = map.toSortedMap { p0, p1 -> p0.personIndex.compareTo(p1.personIndex) }

    val list1 = sortedMap.map { obj -> formatInt(allPersons.find { it.LOL_puuid == obj.key.LOL_puuid }!!.personIndex, 2) + "|" + allPersons.find { it.LOL_puuid == obj.key.LOL_puuid }!!.asUser(guild).lowDescriptor() }
    val listHeroes = sortedMap.map { formatInt(allPersons.find { per -> per.LOL_puuid == it.key.LOL_puuid }!!.personIndex, 2) + "| " + LeagueMainObject.findHeroForKey(it.value.getOrNull(0)?.championId.toString()).name + " / " + LeagueMainObject.findHeroForKey(it.value.getOrNull(1)?.championId.toString()).name + " / " + LeagueMainObject.findHeroForKey(it.value.getOrNull(2)?.championId.toString()).name }
    val listPoints = sortedMap.map { formatInt(allPersons.find { per -> per.LOL_puuid == it.key.LOL_puuid }!!.personIndex, 2) + "| " + it.value.getOrNull(0)?.championPoints.toString() + " / " + it.value.getOrNull(1)?.championPoints + " / " + it.value.getOrNull(2)?.championPoints }

    builder.content = "Статистика по Чемпионам: ${TimeStamp.now()}\n"
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

fun editMessageSimpleContent(
    builder: UserMessageModifyBuilder,
    map: ArrayList<FireMatch>,
    allPersons: ArrayList<FirePerson>
) {
    builder.content = "Статистика по Серверу: ${TimeStamp.now()}\n" +
            "Игр на сервере: ${map.size}\n" +
            "Пользователей в базе: ${allPersons.size}\n" +
            "Версия игры: ${LeagueMainObject.LOL_VERSION}\n" +
            "Количество чемпионов: ${LeagueMainObject.LOL_HEROES}"
}

fun editMessageGlobalStatisticContent(
    builder: UserMessageModifyBuilder,
    allMatches: ArrayList<FireMatch>,
    allPersons: ArrayList<FirePerson>,
    guild: Guild
) {

    val dataList = ArrayList<FireParticipant>()

    allMatches.forEach {match ->
        match.getParticipants(allPersons).forEach { perc ->
            match.listPerc.find { perc.LOL_puuid == it.puuid }?.let {firePart ->
                val findedObj = dataList.find { it.puuid == firePart.puuid }
                if (findedObj == null) {
                    if (firePart.win)
                        firePart.statWins++
                    firePart.statGames++
                    firePart.sortIndex = perc.personIndex

                    dataList.add(firePart)
                } else {
                    findedObj.kills += firePart.kills
                    findedObj.kills2 += firePart.kills2
                    findedObj.kills3 += firePart.kills3
                    findedObj.kills4 += firePart.kills4
                    findedObj.kills5 += firePart.kills5
                    findedObj.skillsCast += firePart.skillsCast
                    findedObj.totalDmgToChampions += firePart.totalDmgToChampions

                    if (firePart.win)
                        findedObj.statWins++
                    findedObj.statGames++
                }
            }
        }
    }

    dataList.sortBy { it.sortIndex }
    val charStr = " / "

    val list1 = dataList.map { obj -> formatInt(allPersons.find { it.LOL_puuid == obj.puuid }!!.personIndex, 2) + "|" + allPersons.find { it.LOL_puuid == obj.puuid }!!.asUser(guild).lowDescriptor() }
    val listGames = dataList.map { formatInt(allPersons.find { per -> per.LOL_puuid == it.puuid }!!.personIndex, 2) + "| " + formatInt(it.statGames, 3) + charStr + formatInt(it.statWins, 3) + charStr + formatInt(((it.statWins.toDouble() / it.statGames.toDouble()) * 100).toInt(), 2) + "%" + charStr + formatInt(it.skillsCast, 5) }
    val listAllKills = dataList.map { formatInt(allPersons.find { per -> per.LOL_puuid == it.puuid }!!.personIndex, 2) + "| " + formatInt(it.kills, 4) + charStr + formatInt(it.kills2, 3) + charStr + formatInt(it.kills3, 2) + charStr + formatInt(it.kills4, 2) + charStr + formatInt(it.kills5, 2) }

    dataList.forEach {
        it.clearData()
    }

    builder.content = "Общая статистика: ${TimeStamp.now()}\n"
    builder.embed {
        field {
            name = "User"
            value = list1.joinToString(separator = "\n")
            inline = true
        }
        field {
            name = "Games/Wins/WinRate/Skills"
            value = listGames.joinToString(separator = "\n")
            inline = true
        }
        field {
            name = "Kills/Double/Tripple/Quadra/Penta"
            value = listAllKills.joinToString(separator = "\n")
            inline = true
        }
    }
}

fun editMessagePentaDataContent(
    builder: UserMessageModifyBuilder,
    allMatches: ArrayList<FireMatch>,
    allPersons: ArrayList<FirePerson>,
    guild: Guild
) {
    val dataList = ArrayList<DataBasic>()
    allMatches.forEach {match ->
        match.getParticipants(allPersons).forEach { perc ->
            match.listPerc.find { perc.LOL_puuid == it.puuid }?.let {firePart ->
                if (firePart.kills5 > 0) dataList.add(DataBasic(user = perc, text = "Сделал пентакилл за '${LeagueMainObject.findHeroForKey(firePart.championId.toString()).name}'", date = match.matchDate))
            }
        }
    }

    dataList.sortByDescending { it.date }

    val list1 = dataList.map { (formatInt(it.user?.personIndex?:-1, 2) + "|" + it.user?.asUser(guild)?.lowDescriptor()) }
    val list2 = dataList.map { it.text }
    val list3 = dataList.map { it.date.toFormatDate() }

    builder.content = "Доска ПЕНТАКИЛЛОВ ${TimeStamp.now()}\n"
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