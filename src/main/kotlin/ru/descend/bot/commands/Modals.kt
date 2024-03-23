package ru.descend.bot.commands

import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.common.entity.TextInputStyle
import dev.kord.core.behavior.interaction.response.respond
import me.jakejmattson.discordkt.commands.subcommand
import me.jakejmattson.discordkt.prompts.promptModal
import ru.descend.bot.postgre.r2dbc.R2DBC
import ru.descend.bot.toStringUID

//fun promptCommands() = subcommand("Prompt", Permissions(Permission.Administrator)) {
//    sub("Modal") {
//        execute {
//            val (responseInteraction, inputs) = promptModal(interaction!!, "Enter Information") {
//                input("ФИО", TextInputStyle.Short, required = true, allowedLength = 3..50)
//                input("Почтовый адрес", TextInputStyle.Paragraph)
//                input("Номер телефона", TextInputStyle.Paragraph, required = false)
//            }
//
//            val guild = R2DBC.getGuild(guild)
//            author.toStringUID()
//
//            val (fio, email, phone) = inputs
//
//            responseInteraction.respond {
//                content = "Hello $fio! $email is a great age $phone"
//            }
//        }
//    }
//}