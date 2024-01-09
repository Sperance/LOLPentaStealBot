package ru.descend.bot.commands

import dev.kord.core.entity.User
import dev.kord.rest.builder.message.EmbedBuilder
import me.jakejmattson.discordkt.commands.commands
import me.jakejmattson.discordkt.extensions.footer
import me.jakejmattson.discordkt.extensions.fullName
import ru.descend.bot.lowDescriptor
import ru.descend.bot.printLog
import ru.descend.bot.toFormatDate
import ru.descend.bot.toFormatDateTime

fun basics() = commands("Basics") {

//    slash("statPStill", "Показать всех Пентастилерров сервера"){
//        execute {
//            printLog("Start command '$name' from ${author.fullName}")
//
//            val fieldsHero = ArrayList<EmbedBuilder.Field>()
//            initializeTitlePStill(fieldsHero)
//
//            val arrayPerson = readPersonFile(guild).listPersons
//            arrayPerson.sortByDescending { it.pentaStills.size }
//            arrayPerson.forEach {person ->
//                if (person.pentaStills.isNotEmpty()) {
//                    addLinePStill(fieldsHero, person.toUser(guild), person.pentaStills.count { it.whoSteal == person.uid }, person.pentaStills.count { it.fromWhomSteal == person.uid })
//                }
//            }
//
//            respond {
//                title = "Доска Пентастиллов"
//                description = "Здесь прям лютые зверюги"
//                fields = fieldsHero
//                footer("Сформировано: ${System.currentTimeMillis().toFormatDateTime()}")
//            }
//        }
//    }
//
//    slash("statPKill", "Показать всех Пентакиллеров сервера") {
//        execute {
//            printLog("Start command '$name' from ${author.fullName}")
//
//            val fieldsHero = ArrayList<EmbedBuilder.Field>()
//            initializeTitlePKill(fieldsHero)
//
//            val arrayPerson = readPersonFile(guild).listPersons
//            arrayPerson.sortByDescending { it.pentaKills.size }
//            arrayPerson.forEach {
//                if (it.pentaKills.isNotEmpty()) {
//                    addLinePKill(fieldsHero, it.toUser(guild), it.pentaKills.size, it.pentaKills.last().date.toFormatDate())
//                }
//            }
//
//            respond {
//                title = "Доска Пентакиллов"
//                description = "Здесь прям красавчики сервера все (и красопеточки)"
//                fields = fieldsHero
//                footer("Сформировано: ${System.currentTimeMillis().toFormatDateTime()}")
//            }
//        }
//    }
}