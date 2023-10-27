package ru.descend.bot.listeners

import dev.kord.core.event.message.MessageCreateEvent
import me.jakejmattson.discordkt.dsl.listeners

//Create a block of listeners.
fun listeners() = listeners {
    on<MessageCreateEvent> {
        require(message.author?.isBot == false)
        println("Author: ${message.author?.username?:""} Created: ${message.content}")
    }
}