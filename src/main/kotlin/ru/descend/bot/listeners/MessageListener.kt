package ru.descend.bot.listeners

import dev.kord.core.event.guild.BanAddEvent
import dev.kord.core.event.guild.BanRemoveEvent
import dev.kord.core.event.guild.MemberJoinEvent
import dev.kord.core.event.guild.MemberLeaveEvent
import dev.kord.core.event.guild.MemberUpdateEvent
import dev.kord.core.event.role.RoleCreateEvent
import dev.kord.core.event.role.RoleDeleteEvent
import dev.kord.core.event.role.RoleUpdateEvent
import me.jakejmattson.discordkt.dsl.listeners
import me.jakejmattson.discordkt.extensions.descriptor
import org.w3c.dom.events.MutationEvent
import ru.descend.bot.printLog

fun listeners() = listeners {

    on<MemberJoinEvent> {
        val memberUser = member.asUser().descriptor()
        printLog(guild.asGuild(), "{Joined to server} $memberUser All: ${guild.asGuild().memberCount}")
    }
    on<MemberLeaveEvent> {
        val member = user.descriptor()
        printLog(guild.asGuild(), "{Leave from server} $member All: ${guild.asGuild().memberCount}")
    }
    on<MemberUpdateEvent> {
        val member = member.asUser().descriptor()
        printLog(guild.asGuild(), "{Updated} $member")
    }

    on<BanAddEvent>{
        printLog(getGuild(), "{Ban add} ${user.descriptor()} ${getBan().getUser().descriptor()} reason: ${getBan().reason}")
    }
    on<BanRemoveEvent>{
        printLog(getGuild(), "{Ban remove} ${user.descriptor()}")
    }

    on<RoleCreateEvent> {
        printLog(getGuild(), "{Create Role} ${role.name} ${role.id.value} ${role.permissions.code.value}")
    }
    on<RoleDeleteEvent> {
        printLog(getGuild(), "{Delete Role} ${role?.name} ${role?.id?.value} ${role?.permissions?.code?.value}")
    }
    on<RoleUpdateEvent> {
        printLog(getGuild(), "{Update Role} ${role.name} ${role.id.value} ${role.permissions.code.value}")
    }
}