package ru.descend.bot.listeners

import dev.kord.core.event.guild.MemberJoinEvent
import dev.kord.core.event.guild.MemberLeaveEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.event.message.MessageDeleteEvent
import me.jakejmattson.discordkt.dsl.listeners
import me.jakejmattson.discordkt.extensions.descriptor
import ru.descend.bot.printLog

fun listeners() = listeners {
    on<MemberJoinEvent> {
        val memberUser = member.asUser().descriptor()
        printLog("[${guild.id}]{Joined to server} $memberUser")
    }
    on<MemberLeaveEvent> {
        val member = user.descriptor()
        printLog("[${guild.id}]{Leave from server} $member")
    }
}