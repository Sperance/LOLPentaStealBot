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
import ru.descend.bot.savedObj.readDataFile
import ru.descend.bot.savedObj.writeDataFile
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
                println("\t  ${it.name}")
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
                if (file.messageId != null) {
                    channelText.getMessageOrNull(Snowflake(file.messageId!!))?.edit {
                        this.content = "Test repeat message ${System.currentTimeMillis().toFormatDateTime()}"
                    }
                } else {
                    val message = channelText.createMessage("Test repeat message ${System.currentTimeMillis().toFormatDateTime()}")
                    file.messageId = message.id.value.toString()
                    writeDataFile(guild, file)
                }
            }
            printLog("Guild message tick ${guild.id}")
            delay(10 * 60 * 1000) //10 minutes
        }
    }
}

fun initializeDataAPI() {
    LeagueMainObject.heroNames = LeagueMainObject.catchHeroNames()
    println("HEROES COUNT: ${LeagueMainObject.heroNames.size}")
}