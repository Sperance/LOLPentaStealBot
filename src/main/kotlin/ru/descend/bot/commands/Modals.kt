package ru.descend.bot.commands

import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.common.entity.TextInputStyle
import dev.kord.core.behavior.interaction.response.respond
import me.jakejmattson.discordkt.commands.subcommand
import me.jakejmattson.discordkt.prompts.promptModal

fun promptCommands() = subcommand("Prompt", Permissions(Permission.UseApplicationCommands)) {
    sub("Account", "Ввести информацию для Бота по текущему аккаунту пользователя Discord") {
        execute {
            val (responseInteraction, inputs) = promptModal(interaction!!, "Информация текущего аккаунта") {
                input("ФИО", TextInputStyle.Short, required = true, allowedLength = 3..50)
                input("Почтовый адрес", TextInputStyle.Paragraph)
                input("Номер телефона", TextInputStyle.Paragraph, required = false)
                input("Дата рождения", TextInputStyle.Paragraph, required = false)
            }

//            val guild = R2DBC.getGuild(guild)
//            author.toStringUID()

            val (fio, email, phone, birthday) = inputs

            responseInteraction.respond {
                content = "Тестирование пройдено успешно: $fio $email $phone $birthday"
            }
        }
    }
}