package ru.descend.bot

import dev.kord.common.entity.Permission
import dev.kord.core.entity.Guild
import dev.kord.core.entity.User
import kotlinx.coroutines.flow.Flow
import ru.descend.bot.data.Configuration
import me.jakejmattson.discordkt.extensions.descriptor

fun User.lowDescriptor(): String {
    return descriptor().split(" :: ")[1]
}

suspend fun <T> Flow<T>.asList(): ArrayList<T> {
    val emptyList = ArrayList<T>()
    collect { emptyList.add(it) }
    return emptyList
}

fun User.isBotOwner(): Boolean {
    return id == Configuration.botOwnerId
}

suspend fun User.checkRoleForName(guild: Guild, name: String): Boolean {
    var result = false
    asMember(guild.id).roles.collect {
        if (it.name.lowercase() == name.lowercase()){
            result = true
            return@collect
        }
    }
    return result
}

suspend fun User.checkPermission(guild: Guild, permission: Permission): Boolean {
    var result = false
    asMember(guild.id).roles.collect {
        if (it.permissions.values.contains(permission)){
            result = true
            return@collect
        }
    }
    return result
}