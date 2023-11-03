package ru.descend.bot.listeners

import dev.kord.core.event.guild.MemberJoinEvent
import dev.kord.core.event.guild.MemberLeaveEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.event.message.MessageDeleteEvent
import me.jakejmattson.discordkt.dsl.listeners
import me.jakejmattson.discordkt.extensions.descriptor
import ru.descend.bot.printLog

fun listeners() = listeners {
    on<MessageCreateEvent> {
        require(message.author?.isBot == false)
        printLog("Author: ${message.author?.username?:""} Message: ${message.content}")
    }

    on<MemberJoinEvent> {
        val member = member.asUser().descriptor()
        printLog("[Joined to server] $member")
    }

    on<MemberLeaveEvent> {
        val member = user.descriptor()
        printLog("[Leave from server] $member")
    }
}