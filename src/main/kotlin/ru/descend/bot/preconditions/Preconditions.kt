package ru.descend.bot.preconditions

import me.jakejmattson.discordkt.dsl.precondition

fun botPrecondition() = precondition {
    if (author.isBot)
        fail("Бот не может этого сделать! [${message?.content}]")
}