package ru.descend.bot

import dev.kord.common.annotation.KordPreview
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.common.entity.PresenceStatus
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.getChannelOf
import dev.kord.core.entity.Guild
import dev.kord.core.entity.User
import dev.kord.core.entity.channel.TextChannel
import dev.kord.gateway.Intents
import dev.kord.gateway.PrivilegedIntent
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.modify.UserMessageModifyBuilder
import dev.kord.rest.builder.message.modify.embed
import dev.kord.x.emoji.Emojis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import me.jakejmattson.discordkt.dsl.CommandException
import me.jakejmattson.discordkt.dsl.ListenerException
import me.jakejmattson.discordkt.dsl.bot
import ru.descend.bot.data.Configuration
import ru.descend.bot.firebase.CompleteResult
import ru.descend.bot.firebase.F_PENTAKILLS
import ru.descend.bot.firebase.F_PENTASTILLS
import ru.descend.bot.firebase.F_USERS
import ru.descend.bot.firebase.FireGuild
import ru.descend.bot.firebase.FirePKill
import ru.descend.bot.firebase.FirePSteal
import ru.descend.bot.firebase.FirePerson
import ru.descend.bot.firebase.FirebaseService
import ru.descend.bot.lolapi.LeagueMainObject
import ru.descend.bot.lolapi.leaguedata.match_dto.MatchDTO
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
                FirebaseService.addGuild(it)
                println("\t  ${it.name} [${it.id.value}]")
                showMainGuildMessage(it)
                showLeagueHistory(it)
            }
        }
    }
}

suspend fun showLeagueHistory(guild: Guild) {
    CoroutineScope(Dispatchers.IO).launch {
        while (true) {
            val allPersons = FirebaseService.getArrayFromCollection<FirePerson>(FirebaseService.collectionGuild(guild, F_USERS))
            val listPUUID = ArrayList<String>()
            allPersons.forEach {
                listPUUID.add(it.LOL_puuid)
            }

            listPUUID.forEach { puuid ->
                LeagueMainObject.catchMatchID(puuid).forEach { matchId ->
                    LeagueMainObject.catchMatch(matchId)?.let { match ->
                        when (val res = FirebaseService.addMatchToUser(guild, allPersons.find { it.LOL_puuid == puuid }!!, match)) {
                            is CompleteResult.Error -> println(res.errorText)
                            is CompleteResult.Success -> null
                        }
                    }
                }
            }

            delay(60 * 60 * 1000) //60min
        }
    }
}

suspend fun showMainGuildMessage(guild: Guild) {
    CoroutineScope(Dispatchers.IO).launch {
        while (true) {

            var guildData = FirebaseService.getGuild(guild)
            if (guildData == null) {
                FirebaseService.addGuild(guild)
                guildData = FirebaseService.getGuild(guild)
            }

            if (guildData!!.botChannelId.isNotEmpty()) {
                val channelText = guild.getChannelOf<TextChannel>(Snowflake(guildData.botChannelId))

                val fieldDateStats = ArrayList<EmbedBuilder.Field>()

                //Инициализация верхней шапки таблицы
                initializeTitleTimeStats(fieldDateStats)

                //Заполнение нижней части таблицы
                catchAndParseDateFile(guild, fieldDateStats)

                val builder = EmbedBuilder()
                builder.fields = fieldDateStats

                try {
                    if (guildData.messageId.isNotEmpty()) {
                        editMessage(channelText, fieldDateStats, guildData)
                    } else {
                        createMessage(channelText, fieldDateStats, guildData)
                    }
                } catch (e: Exception) {
                    println("Error: ${e.message}")
                }
            }
            delay(5 * 60 * 1000) //10 min
        }
    }
}

suspend fun editMessage(
    channelText: TextChannel,
    fieldDateStats: ArrayList<EmbedBuilder.Field>,
    file: FireGuild
) {
    val message = channelText.getMessageOrNull(Snowflake(file.messageId))
    if (message != null) {
        message.edit { editMessageContent(this, fieldDateStats) }
    } else {
        createMessage(channelText, fieldDateStats, file)
    }
}

suspend fun createMessage(
    channelText: TextChannel,
    fieldDateStats: ArrayList<EmbedBuilder.Field>,
    file: FireGuild
) {
    val message = channelText.createMessage("Initial Message")
    channelText.getMessage(message.id).edit { editMessageContent(this, fieldDateStats) }
    file.messageId = message.id.value.toString()
    printLog(file.fireSaveData())
}

fun editMessageContent(
    builder: UserMessageModifyBuilder,
    fieldDateStats: ArrayList<EmbedBuilder.Field>
) {
    builder.content = "Обновлено: ${System.currentTimeMillis().toFormatDateTime()}\n"
    builder.embed {
        this.fields = fieldDateStats
    }
}

fun catchAndParseDateFile(guild: Guild, field: ArrayList<EmbedBuilder.Field>) {
    val basicData = ArrayList<DataBasic>()

    val allPersons = FirebaseService.getArrayFromCollection<FirePerson>(FirebaseService.collectionGuild(guild, F_USERS))
    allPersons.forEach {person ->
        FirebaseService.getArrayFromCollection<FirePKill>(person.toDocument().collection(F_PENTAKILLS)).forEach { pKill ->
            val textDate = "Сделал Пенту за героя '${pKill.hero?.name}'"
            basicData.add(DataBasic(user = person, text = textDate, date = pKill.SYS_CREATE_DATE))
        }
        FirebaseService.getArrayFromCollection<FirePSteal>(person.toDocument().collection(F_PENTASTILLS)).forEach { pStill ->
            val textDate = if (pStill.whoSteal == null) {
                "Ноунейм на '${pStill.hero?.name}' состилил пенту. Грусть"
            } else if (pStill.fromWhomSteal == null) {
                "За '${pStill.hero?.name}' состилил пенту у Ноунейма. Мосчь"
            } else {
                if (pStill.whoSteal!!.snowflake == person.KORD_id)
                    "Состилил пенту за '${pStill.hero?.name}' у ${pStill.fromWhomSteal!!.asUser(guild).lowDescriptor()}. Соболезнуем"
                else
                    ""
            }
            if (textDate.isNotEmpty())
                basicData.add(DataBasic(user = person, text = textDate, date = pStill.SYS_CREATE_DATE))
        }
    }

    basicData.sortByDescending { it.date }
    basicData.forEach {
        addLineTimeStats(field, it.user!!.asUser(guild), it.text, it.date)
    }
}

fun initializeTitleTimeStats(field: ArrayList<EmbedBuilder.Field>) {
    field.add(0, EmbedBuilder.Field().apply { name = "Призыватель"; value = ""; inline = true })
    field.add(1, EmbedBuilder.Field().apply { name = "Суета"; value = ""; inline = true })
    field.add(2, EmbedBuilder.Field().apply { name = "Дата"; value = ""; inline = true })
}

fun addLineTimeStats(
    field: ArrayList<EmbedBuilder.Field>,
    user: User,
    mainText: String,
    date: Long
) {
    field.add(field.size, EmbedBuilder.Field().apply { name = ""; value = user.lowDescriptor(); inline = true })
    field.add(field.size, EmbedBuilder.Field().apply { name = ""; value = mainText; inline = true })
    field.add(field.size, EmbedBuilder.Field().apply { name = ""; value = date.toFormatDate(); inline = true })
}

fun initializeDataAPI() {
    LeagueMainObject.heroNames = LeagueMainObject.catchHeroNames()
    println("HEROES COUNT: ${LeagueMainObject.heroNames.size}")
}