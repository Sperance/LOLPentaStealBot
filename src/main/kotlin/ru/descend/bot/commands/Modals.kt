package ru.descend.bot.commands

import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.common.entity.TextInputStyle
import dev.kord.core.behavior.interaction.response.respond
import me.jakejmattson.discordkt.commands.subcommand
import me.jakejmattson.discordkt.prompts.promptModal

fun promptCommands() = subcommand("Prompt", Permissions(Permission.Administrator)) {
    sub("Modal") {
        execute {
            val (responseInteraction, inputs) = promptModal(interaction!!, "Enter Information") {
                input("Name", TextInputStyle.Short)
                input("Age", TextInputStyle.Short)
            }

            val (name, age) = inputs

            responseInteraction.respond {
                content = "Hello $name! $age is a great age"
            }
        }
    }
}