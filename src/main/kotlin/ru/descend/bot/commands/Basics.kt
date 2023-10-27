package ru.descend.bot.commands

import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import me.jakejmattson.discordkt.commands.commands
import me.jakejmattson.discordkt.extensions.pfpUrl

fun basics() = commands("Basics") {
    slash("stats", "Show the all list of Users with Pentastill`s") {
        execute {

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