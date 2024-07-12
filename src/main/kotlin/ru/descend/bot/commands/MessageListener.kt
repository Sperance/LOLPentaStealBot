package ru.descend.bot.commands

import dev.kord.core.event.guild.BanAddEvent
import dev.kord.core.event.guild.BanRemoveEvent
import dev.kord.core.event.guild.MemberJoinEvent
import dev.kord.core.event.guild.MemberLeaveEvent
import dev.kord.core.event.guild.MemberUpdateEvent
import dev.kord.core.event.role.RoleCreateEvent
import dev.kord.core.event.role.RoleDeleteEvent
import me.jakejmattson.discordkt.dsl.listeners
import me.jakejmattson.discordkt.util.descriptor
import ru.descend.bot.generateAIText
import ru.descend.bot.lowDescriptor
import ru.descend.bot.postgre.R2DBC
import ru.descend.bot.printLog
import ru.descend.bot.sendMessage

fun listeners() = listeners {
    on<MemberJoinEvent> {
        val memberUser = member.asUser().descriptor()

        val generatedText = generateAIText("Напиши необычное, но с юмором приветственное сообщение пользователю ${member.asUser().lowDescriptor()} который зашел на Discord сервер, посвященному игре League of Legends")
        val addedText = "\nТак же для получения Плюшек не забудь сообщить Модераторам день/месяц рождения и предпочтительную Роль на сервере. Спасибо."

        printLog(getGuild(), "{Зашел на сервер} $memberUser\n$generatedText")
        getGuild().sendMessage(R2DBC.getGuild(getGuild()).messageIdDebug, "{Зашел на сервер} ${member.asUser().descriptor()}\n$generatedText")
        getGuild().sendMessage(R2DBC.getGuild(getGuild()).messageIdStatus, "$generatedText\n$addedText")
    }
    on<MemberLeaveEvent> {
        val textLeave = "Странный пользователь ${user.descriptor()} покинул наш сервер ._."
        printLog(getGuild(), textLeave)
        getGuild().sendMessage(R2DBC.getGuild(getGuild()).messageIdDebug, textLeave)
        getGuild().sendMessage(R2DBC.getGuild(getGuild()).messageIdStatus, textLeave)
    }
    on<MemberUpdateEvent> {
        val member = member.asUser().descriptor()
        printLog(getGuild(), "{Updated} $member")
    }

    on<BanAddEvent>{
        printLog(getGuild(), "{Ban add} ${user.descriptor()} ${getBan().getUser().descriptor()} reason: ${getBan().reason}")
        getGuild().sendMessage(R2DBC.getGuild(getGuild()).messageIdDebug, "{Ban add} from ${user.lowDescriptor()} to ${getBan().getUser().lowDescriptor()} reason: ${getBan().reason}")
    }
    on<BanRemoveEvent>{
        printLog(getGuild(), "{Ban remove} ${user.descriptor()}")
    }

    on<RoleCreateEvent> {
        printLog(getGuild(), "{Create Role} ${role.name} ${role.id.value} ${role.permissions.code.value}")
        getGuild().sendMessage(R2DBC.getGuild(guild.asGuild()).messageIdDebug, "{Create Role} ${role.name} ${role.id.value}")
    }
    on<RoleDeleteEvent> {
        printLog(getGuild(), "{Delete Role} ${role?.name} ${role?.id?.value} ${role?.permissions?.code?.value}")
        getGuild().sendMessage(R2DBC.getGuild(guild.asGuild()).messageIdDebug, "{Delete Role} ${role?.name} ${role?.id?.value} ${role?.permissions?.code?.value}")
    }
}