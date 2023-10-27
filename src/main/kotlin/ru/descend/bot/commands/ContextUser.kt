package ru.descend.bot.commands

import ru.descend.bot.lowDescriptor
import me.jakejmattson.discordkt.commands.commands
import me.jakejmattson.discordkt.extensions.addField
import me.jakejmattson.discordkt.extensions.footer

//Вызывается по правому клику по любому пользователю сервера
fun contextUser() = commands("Context") {
    user(displayText = "ПентаСтиллер", slashName = "AddPentaSteal", description = "Write a Pentasteal for current user") {
        val text = "AddPentaSteal Cliced on: ${arg.lowDescriptor()} from: ${author.lowDescriptor()}"
        respond {
            title = "Произошел ПЕНТАСТИЛЛ"
            addField("Красавелла: ", arg.lowDescriptor())
            footer("Всего пентастиллов: 2")
        }
    }
    user(displayText = "ПентаКиллер", slashName = "AddPentaKill", description = "Write a PentaKill for current user") {
        val text = "AddPentaKill Cliced on: ${arg.lowDescriptor()} from: ${author.lowDescriptor()}"
        respond {
            title = "Произошел ПЕНТАКИЛЛ"
            addField("Состилил пятерых: ", arg.lowDescriptor())
            footer("Всего пентакиллов: 3")
        }
    }
}