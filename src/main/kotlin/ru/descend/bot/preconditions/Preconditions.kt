package ru.descend.bot.preconditions

import me.jakejmattson.discordkt.dsl.precondition

fun botPrecondition() = precondition {
    if (author.isBot)
        fail("Bots cannot do this! [${this.message?.content}]")
}