package ru.descend.bot.commands

import me.jakejmattson.discordkt.commands.commands
import me.jakejmattson.discordkt.extensions.fullName
import me.jakejmattson.discordkt.extensions.pfpUrl

fun basics() = commands("Basics") {
    slash("stats", "Show the all list of Users with Pentastill`s") {
        execute {

            println("Start command '$name' from ${author.fullName}")

            val owner = author
            respond {

                title = owner.username + " ${guild.data.memberCount}"
                description = "My favorite number is"

                thumbnail {
                    url = owner.pfpUrl
                }
            }
        }
    }
}