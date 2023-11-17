package ru.descend.bot

import dev.kord.common.annotation.KordPreview
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.common.entity.PresenceStatus
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.getChannelOf
import dev.kord.core.entity.Guild
import dev.kord.core.entity.Message
import dev.kord.core.entity.User
import dev.kord.core.entity.channel.Channel
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
import ru.descend.bot.lolapi.LeagueMainObject
import ru.descend.bot.lolapi.champions.InterfaceChampionBase
import ru.descend.bot.savedObj.DataBasic
import ru.descend.bot.savedObj.DataFile
import ru.descend.bot.savedObj.readDataFile
import ru.descend.bot.savedObj.readPersonFile
import ru.descend.bot.savedObj.writeDataFile
import java.awt.Color
import java.io.File

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
                showMainGuildMessage(it)
            }
        }
    }
}

suspend fun showMainGuildMessage(guild: Guild) {
    CoroutineScope(Dispatchers.IO).launch {
        while (true){
            val file = readDataFile(guild)
            if (file.botChannelId != null){
                val channelText = guild.getChannelOf<TextChannel>(Snowflake(file.botChannelId!!))

                val fieldDateStats = ArrayList<EmbedBuilder.Field>()

                //Инициализация верхней шапки таблицы
                initializeTitleTimeStats(fieldDateStats)

                //Заполнение нижней части таблицы
                catchAndParseDateFile(guild, fieldDateStats)

                val builder = EmbedBuilder()
                builder.fields = fieldDateStats

                try {
                    if (file.messageId != null) {
                        editMessage(guild, channelText, fieldDateStats, file)
                    } else {
                        createMessage(guild, channelText, fieldDateStats, file)
                    }
                } catch (e: Exception) {
                    println("Error: ${e.message}")
                }
            }
//            println("[${guild.id}] tick")
            delay(10 * 60 * 1000) //10 min
        }
    }
}

suspend fun editMessage(guild: Guild, channelText: TextChannel, fieldDateStats: ArrayList<EmbedBuilder.Field>, file: DataFile){
    val message = channelText.getMessageOrNull(Snowflake(file.messageId!!))
    if (message != null){
        message.edit {editMessageContent(this, fieldDateStats)}
    } else {
        createMessage(guild, channelText, fieldDateStats, file)
    }
}

suspend fun createMessage(guild: Guild, channelText: TextChannel, fieldDateStats: ArrayList<EmbedBuilder.Field>, file: DataFile){
    val message = channelText.createMessage("Initial Message")
    channelText.getMessage(message.id).edit {editMessageContent(this, fieldDateStats)}
    file.messageId = message.id.value.toString()
    writeDataFile(guild, file)
}

fun editMessageContent(builder: UserMessageModifyBuilder, fieldDateStats: ArrayList<EmbedBuilder.Field>) {
    builder.content = "Обновлено: ${System.currentTimeMillis().toFormatDateTime()}\n"
    builder.embed {
        this.fields = fieldDateStats
    }
}

fun catchAndParseDateFile(guild: Guild, field: ArrayList<EmbedBuilder.Field>){
    val fileData = readPersonFile(guild)
    val basicData = ArrayList<DataBasic>()
    val findObjHero = LeagueMainObject.heroObjects as ArrayList<InterfaceChampionBase>
    fileData.listPersons.forEach { person ->
        person.pentaKills.forEach { pKill ->
            val textDate = "сделал Пенту за героя '${findObjHero.find { it.key == pKill.heroKey }!!.name}'"
            basicData.add(DataBasic(user = person, text = textDate, date = pKill.date))
        }
        person.pentaStills.forEach { pStill ->
            val textDate = if (pStill.whoSteal == "0") {
                "Ноунейм на '${findObjHero.find { it.key == pStill.heroSteal }!!.name}' состилил пенту. Грусть"
            } else if (pStill.fromWhomSteal == "0") {
                "За '${findObjHero.find { it.key == pStill.heroSteal }!!.name}' состилил пенту у Ноунейма. Мосчь"
            } else {
                "Состилил пенту за '${findObjHero.find { it.key == pStill.heroSteal }!!.name}' у ${pStill.fromWhomSteal}. Соболезнуем"
            }
            basicData.add(DataBasic(user = person, text = textDate, date = pStill.date))
        }
    }

    basicData.sortByDescending { it.date }
    basicData.forEach {
        addLineTimeStats(field, it.user.toUser(guild), it.text, it.date)
    }
}

fun initializeTitleTimeStats(field: ArrayList<EmbedBuilder.Field>) {
    field.add(0, EmbedBuilder.Field().apply { name = "Призыватель"; value = ""; inline = true })
    field.add(1, EmbedBuilder.Field().apply { name = "Суета"; value = ""; inline = true })
    field.add(1, EmbedBuilder.Field().apply { name = "Дата"; value = ""; inline = true })
}

fun addLineTimeStats(field: ArrayList<EmbedBuilder.Field>, user: User, mainText: String, date: Long) {
    field.add(field.size, EmbedBuilder.Field().apply { name = ""; value = user.lowDescriptor(); inline = true })
    field.add(field.size, EmbedBuilder.Field().apply { name = ""; value = mainText; inline = true })
    field.add(field.size, EmbedBuilder.Field().apply { name = ""; value = date.toFormatDate(); inline = true })
}

fun initializeDataAPI() {
    LeagueMainObject.heroNames = LeagueMainObject.catchHeroNames()
    println("HEROES COUNT: ${LeagueMainObject.heroNames.size}")
}