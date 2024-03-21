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
import ru.descend.bot.lowDescriptor
import ru.descend.bot.postgre.r2dbc.R2DBC
import ru.descend.bot.printLog
import ru.descend.bot.sendMessage

fun listeners() = listeners {

    on<MemberJoinEvent> {
        val memberUser = member.asUser().descriptor()
        printLog(guild.asGuild(), "{Зашел на сервер} $memberUser")
        guild.asGuild().sendMessage(R2DBC.getGuild(guild.asGuild()).messageIdDebug, "{Зашел на сервер} ${member.asUser().descriptor()}")
    }
    on<MemberLeaveEvent> {
        var textLeave = "{Вышел с сервера} ${user.descriptor()}"
        printLog(getGuild(), textLeave)
        guild.asGuild().sendMessage(R2DBC.getGuild(getGuild()).messageIdDebug, textLeave)
    }
    on<MemberUpdateEvent> {
        val member = member.asUser().descriptor()
        printLog(guild.asGuild(), "{Updated} $member")
    }

    on<BanAddEvent>{
        printLog(getGuild(), "{Ban add} ${user.descriptor()} ${getBan().getUser().descriptor()} reason: ${getBan().reason}")
        guild.asGuild().sendMessage(R2DBC.getGuild(guild.asGuild()).messageIdDebug, "{Ban add} from ${user.lowDescriptor()} to ${getBan().getUser().lowDescriptor()} reason: ${getBan().reason}")
    }
    on<BanRemoveEvent>{
        printLog(getGuild(), "{Ban remove} ${user.descriptor()}")
    }

    on<RoleCreateEvent> {
        printLog(getGuild(), "{Create Role} ${role.name} ${role.id.value} ${role.permissions.code.value}")
        guild.asGuild().sendMessage(R2DBC.getGuild(guild.asGuild()).messageIdDebug, "{Create Role} ${role.name} ${role.id.value}")
    }
    on<RoleDeleteEvent> {
        printLog(getGuild(), "{Delete Role} ${role?.name} ${role?.id?.value} ${role?.permissions?.code?.value}")
        guild.asGuild().sendMessage(R2DBC.getGuild(guild.asGuild()).messageIdDebug, "{Delete Role} ${role?.name} ${role?.id?.value} ${role?.permissions?.code?.value}")
    }
    on<RoleUpdateEvent> {
        printLog(getGuild(), "{Update Role} ${role.name} ${role.id.value} ${role.permissions.code.value}")
    }
}