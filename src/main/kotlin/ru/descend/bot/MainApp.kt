package ru.descend.bot

import dev.kord.common.annotation.KordPreview
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.common.entity.PresenceStatus
import dev.kord.gateway.Intents
import dev.kord.x.emoji.Emojis
import me.jakejmattson.discordkt.dsl.CommandException
import me.jakejmattson.discordkt.dsl.ListenerException
import me.jakejmattson.discordkt.dsl.bot
import ru.descend.bot.data.Configuration
import java.awt.Color

@KordPreview
fun main() {
    //Get the bot token from the command line (or your preferred way).
    //Start the bot and set configuration options.
    bot(catchToken()) {

//        val configuration = data("config/config.json") { BotConfiguration() }

        prefix {
            Configuration.prefix
        }

        //Simple configuration options
        configure {
            //Allow a mention to be used in front of commands ('@Bot help').
//            mentionAsPrefix = true

            //Whether to show registered entity information on startup.
            logStartup = true

            //Whether to generate documentation for registered commands.
            documentCommands = true

            //Whether to recommend commands when an invalid one is invoked.
            recommendCommands = true

            //Allow users to search for a command by typing 'search <command name>'.
            searchCommands = true

            //Remove a command invocation message after the command is executed.
            deleteInvocation = true

            //Allow slash commands to be invoked as text commands.
            dualRegistry = true

            //An emoji added when a command is invoked (use 'null' to disable this).
            commandReaction = Emojis.adult

            //A color constant for your bot - typically used in embeds.
            theme = Color(0x00B92F)

            //Configure the Discord Gateway intents for your bot.
            intents = Intents.nonPrivileged

            //Set the default permission required for slash commands.
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

        //The Discord presence shown on your bot.
        presence {
            this.status = PresenceStatus.Online
            listening("красоту AiSelina")
        }

        //This is run once the bot has finished setup and logged in.
        onStart {
            println("Bot ${this.properties.bot.name} started")
        }
    }
}