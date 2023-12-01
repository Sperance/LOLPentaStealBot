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
import kotlinx.coroutines.flow.forEach
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import me.jakejmattson.discordkt.dsl.CommandException
import me.jakejmattson.discordkt.dsl.ListenerException
import me.jakejmattson.discordkt.dsl.bot
import me.jakejmattson.discordkt.extensions.TimeStamp
import ru.descend.bot.data.Configuration
import ru.descend.bot.firebase.F_MATCHES
import ru.descend.bot.firebase.F_PENTAKILLS
import ru.descend.bot.firebase.F_PENTASTILLS
import ru.descend.bot.firebase.F_USERS
import ru.descend.bot.firebase.FireGuild
import ru.descend.bot.firebase.FireMatch
import ru.descend.bot.firebase.FirePKill
import ru.descend.bot.firebase.FirePSteal
import ru.descend.bot.firebase.FireParticipant
import ru.descend.bot.firebase.FirePerson
import ru.descend.bot.firebase.FirebaseService
import ru.descend.bot.lolapi.LeagueMainObject
import ru.descend.bot.savedObj.DataBasic
import java.awt.Color

@OptIn(PrivilegedIntent::class)
@KordPreview
fun main() {
    println("Initializing is Started")
    initializeDataAPI()
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
            kord.guilds.toList().forEach {
                println("\t  ${it.name} [${it.id.value}]")

                var guildData = FirebaseService.getGuild(it)
                if (guildData == null) {
                    FirebaseService.addGuild(it)
                    guildData = FirebaseService.getGuild(it)
                }

                removeMessage(it)
                showMainGuildMessage(it, guildData!!)
                showLeagueHistory(it)
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
            if (msgId == guildData.messageIdPentaData || msgId == guildData.messageIdGlobalStatisticData) {

            } else {
                it.delete()
            }
        }
        printLog("Clean channel end ${guild.id.value}")
    }
}

suspend fun showLeagueHistory(guild: Guild) {
    CoroutineScope(Dispatchers.IO).launch {
        while (true) {
            FirebaseService.getArrayFromCollection<FirePerson>(FirebaseService.collectionGuild(guild, F_USERS)).forEach {
                if (it.LOL_puuid == "") return@forEach
                LeagueMainObject.catchMatchID(it.LOL_puuid).forEach { matchId ->
                    LeagueMainObject.catchMatch(matchId)?.let { match ->
                        FirebaseService.addMatchToGuild(guild, match)
                    }
                }
            }
            delay(60 * 60 * 1000) //60min
        }
    }
}

suspend fun showMainGuildMessage(guild: Guild, guildData: FireGuild) {
    CoroutineScope(Dispatchers.IO).launch {
        while (true) {
            if (guildData.botChannelId.isNotEmpty()) {
                val channelText = guild.getChannelOf<TextChannel>(Snowflake(guildData.botChannelId))
                val allMatches = FirebaseService.getArrayFromCollection<FireMatch>(FirebaseService.collectionGuild(guild, F_MATCHES))

                editMessageGlobal(channelText, guildData.messageIdPentaData, {
                    editMessagePentaDataContent(it, allMatches, guild)
                }) {
                    createMessagePentaData(channelText, allMatches, guildData)
                }

                editMessageGlobal(channelText, guildData.messageIdGlobalStatisticData, {
                    editMessageGlobalStatisticContent(it, allMatches, guild)
                }) {
                    createMessageGlobalStatistic(channelText, allMatches, guildData)
                }
            }
            delay(30 * 60 * 1000) //30 min
        }
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
    file: FireGuild
) {
    val message = channelText.createMessage("Initial Message")
    channelText.getMessage(message.id).edit { editMessagePentaDataContent(this, allMatches, message.getGuild()) }
    file.messageIdPentaData = message.id.value.toString()
    printLog(file.fireSaveData())
}

suspend fun createMessageGlobalStatistic(
    channelText: TextChannel,
    allMatches: ArrayList<FireMatch>,
    file: FireGuild
) {
    val message = channelText.createMessage("Initial Message")
    channelText.getMessage(message.id).edit { editMessageGlobalStatisticContent(this, allMatches, message.getGuild()) }
    file.messageIdGlobalStatisticData = message.id.value.toString()
    printLog(file.fireSaveData())
}

fun editMessageGlobalStatisticContent(
    builder: UserMessageModifyBuilder,
    allMatches: ArrayList<FireMatch>,
    guild: Guild
) {
    val allPersons = FirebaseService.getArrayFromCollection<FirePerson>(FirebaseService.collectionGuild(guild, F_USERS))

    val dataList = ArrayList<FireParticipant>()

    allMatches.forEach {match ->
        match.getParticipants(guild).forEach { perc ->
            match.listPerc.find { perc.LOL_puuid == it.puuid }?.let {firePart ->
                val findedObj = dataList.find { it.puuid == firePart.puuid }
                if (findedObj == null) {
                    if (firePart.win)
                        firePart.statWins++
                    firePart.statGames++

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

    dataList.sortBy { it.puuid }
    val charStr = " / "

    val list1 = dataList.map { obj -> allPersons.find { it.LOL_puuid == obj.puuid }!!.asUser(guild).lowDescriptor() }
//    val listkills = dataList.map { it.kills }
//    val listkills3 = dataList.map { it.kills3 }
//    val listkills4 = dataList.map { it.kills4 }
//    val listkills5 = dataList.map { it.kills5 }
//    val listskillsCast = dataList.map { it.skillsCast }
    val listGames = dataList.map { formatInt(it.statGames, 3) + charStr + formatInt(it.statWins, 3) + charStr + formatInt(it.skillsCast, 5) }
    val listAllKills = dataList.map { formatInt(it.kills, 4) + charStr + formatInt(it.kills2, 3) + charStr + formatInt(it.kills3, 2) + charStr + formatInt(it.kills4, 2) + charStr + formatInt(it.kills5, 2) }

    builder.content = "Общая статистика: ${TimeStamp.now()}\n"
    builder.embed {
        field {
            name = "User"
            value = list1.joinToString(separator = "\n")
            inline = true
        }
        field {
            name = "Games/Wins/Skills"
            value = listGames.joinToString(separator = "\n")
            inline = true
        }
//        field {
//            name = "Wins"
//            value = listWins.joinToString(separator = "\n")
//            inline = true
//        }
        field {
            name = "K/2/3/4/5"
            value = listAllKills.joinToString(separator = "\n")
            inline = true
        }
//        field {
//            name = "Kill3"
//            value = listkills3.joinToString(separator = "\n")
//            inline = true
//        }
//        field {
//            name = "Kill4"
//            value = listkills4.joinToString(separator = "\n")
//            inline = true
//        }
//        field {
//            name = "Kill5"
//            value = listkills5.joinToString(separator = "\n")
//            inline = true
//        }
//        field {
//            name = "Skills"
//            value = listskillsCast.joinToString(separator = "\n")
//            inline = true
//        }
    }
}

fun editMessagePentaDataContent(
    builder: UserMessageModifyBuilder,
    allMatches: ArrayList<FireMatch>,
    guild: Guild
) {
    val dataList = ArrayList<DataBasic>()
    allMatches.forEach {match ->
        match.getParticipants(guild).forEach { perc ->
            match.listPerc.find { perc.LOL_puuid == it.puuid }?.let {firePart ->
                if (firePart.kills5 > 0) dataList.add(DataBasic(user = perc, text = "Сделал пентакилл за '${LeagueMainObject.findHeroForKey(firePart.championId.toString()).name}'", date = match.matchDate))
            }
        }
    }

    dataList.sortByDescending { it.date }

    val list1 = dataList.map { it.user?.asUser(guild)?.lowDescriptor()?: "" }
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

fun initializeDataAPI() {
    LeagueMainObject.heroNames = LeagueMainObject.catchHeroNames()
    println("HEROES COUNT: ${LeagueMainObject.heroNames.size}")
}