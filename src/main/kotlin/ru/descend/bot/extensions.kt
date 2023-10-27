package ru.descend.bot

import dev.kord.common.entity.Permission
import dev.kord.core.entity.Guild
import dev.kord.core.entity.User
import kotlinx.coroutines.flow.Flow
import ru.descend.bot.data.Configuration
import me.jakejmattson.discordkt.extensions.descriptor
import ru.descend.bot.listeners.DSC_PS
import ru.descend.bot.listeners.decrypt
import ru.descend.bot.listeners.encrypt
import java.io.File
import java.util.Base64

fun User.lowDescriptor(): String {
    return descriptor().split(" :: ")[1]
}

fun catchToken(): String {
    val file = File("token.dsc")
    if (!file.exists()) {
        file.createNewFile()
        //TODO Encrypted token write to file
    }

    return decrypt(file.readBytes(), DSC_PS).decodeToString()
}

fun String.toBase64() : String {
    return Base64.getEncoder().encodeToString(toByteArray())
}

fun String.fromBase64(): String {
    val decodedBytes = Base64.getDecoder().decode(this)
    return String(decodedBytes)
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