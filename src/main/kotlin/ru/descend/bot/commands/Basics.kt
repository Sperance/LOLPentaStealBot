package ru.descend.bot.commands

import dev.kord.core.entity.User
import dev.kord.rest.builder.message.EmbedBuilder
import me.jakejmattson.discordkt.commands.commands
import me.jakejmattson.discordkt.extensions.footer
import me.jakejmattson.discordkt.extensions.fullName
import ru.descend.bot.lowDescriptor
import ru.descend.bot.savedObj.readDataFile
import ru.descend.bot.toFormatDate
import ru.descend.bot.toFormatDateTime

fun basics() = commands("Basics") {

    slash("statPStill", "Показать всех пентастилерров сервера"){
        execute {
            println("Start command '$name' from ${author.fullName}")

            respond("Да не тыкайся ты сюда, еще не готово")
        }
    }

    slash("statPKill", "Показать всех пентакиллеров сервера") {
        execute {
            println("Start command '$name' from ${author.fullName}")

            val fieldsHero = ArrayList<EmbedBuilder.Field>()
            initializeTitlePKill(fieldsHero)

            val arrayPerson = readDataFile(guild).listPersons
            arrayPerson.sortByDescending { it.pentaKills.size }
            arrayPerson.forEach {
                if (it.pentaKills.isNotEmpty()) {
                    addLinePKill(fieldsHero, it.toUser(guild), it.pentaKills.size, it.pentaKills.last().date.toFormatDate())
                }
            }

            respond {
                title = "Доска Пентакиллов"
                description = "Здесь прям красавчики сервера все (и красопеточки)"
                fields = fieldsHero
                footer("Сформировано: ${System.currentTimeMillis().toFormatDateTime()}")
            }
        }
    }
}

fun initializeTitlePKill(field: ArrayList<EmbedBuilder.Field>) {
    field.add(0, EmbedBuilder.Field().apply { name = "Призыватель"; value = ""; inline = true })
    field.add(1, EmbedBuilder.Field().apply { name = "Пентакиллов"; value = ""; inline = true })
    field.add(2, EmbedBuilder.Field().apply { name = "Дата последнего"; value = ""; inline = true })
}

fun addLinePKill(field: ArrayList<EmbedBuilder.Field>, user: User, pentaCount: Int, lastDate: String) {
    field.add(field.size, EmbedBuilder.Field().apply { name = ""; value = user.lowDescriptor(); inline = true })
    field.add(field.size, EmbedBuilder.Field().apply { name = ""; value = pentaCount.toString(); inline = true })
    field.add(field.size, EmbedBuilder.Field().apply { name = ""; value = lastDate; inline = true })
}