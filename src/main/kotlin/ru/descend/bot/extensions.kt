package ru.descend.bot

import dev.kord.common.entity.Permission
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.getChannelOf
import dev.kord.core.entity.Guild
import dev.kord.core.entity.User
import dev.kord.core.entity.channel.TextChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import me.jakejmattson.discordkt.extensions.descriptor
import ru.descend.bot.lolapi.LeagueMainObject
import ru.descend.bot.postgre.SQLData
import java.io.File
import java.text.SimpleDateFormat
import java.util.Base64
import java.util.Date
import java.util.Locale
import kotlin.math.pow

fun printLog(message: Any){
    val curDTime = System.currentTimeMillis().toFormatDateTime()
    println("[$curDTime] $message")
}

suspend fun Guild?.sendMessage(messageId: String, message: String, afterLaunchBody: (() -> Unit)? = null) {
    if (this == null) return
    if (messageId.isEmpty()) return
    if (message.isEmpty()) return
    launch {
        try {
            val channelText = getChannelOf<TextChannel>(Snowflake(messageId))
            channelText.createMessage {
                content = message
            }
        }catch (e: Exception) {
            printLog(this@sendMessage, "Not sended message $message for channel $messageId. Error: ${e.message}")
        }
    }.invokeOnCompletion {
        afterLaunchBody?.invoke()
    }
}

fun launch(block: suspend CoroutineScope.() -> Unit) = CoroutineScope(Dispatchers.IO).launch {
    block.invoke(this)
}

fun asyncLaunch(block: suspend CoroutineScope.() -> Unit) = CoroutineScope(Dispatchers.IO).launch {
    async { block.invoke(this) }
}

fun printLog(guild: Guild, message: Any){
    val curDTime = System.currentTimeMillis().toFormatDateTime()
    println("[$curDTime] [${guild.id.value}] $message")
}

fun Int.toFormatK() : String {
    var index = 0
    var ost = 0
    var num = this
    while (num >= 1000) {
        num /= 1000
        index++
        ost = (this % (1000.0.pow(index.toDouble()))).toString().substring(0, 1).toInt()
    }
    var sumK = ""
    for (i in 1..index){ sumK += "k" }
    val strOst = if (ost != 0) ".$ost" else ""
    return "$num$strOst$sumK"
}

fun Long.toFormatDate() : String {
    return SimpleDateFormat("dd.MM.yy", Locale.getDefault()).format(Date(this))
}

fun Long.toDate() : Date {
    return Date(this)
}

fun Double.format(digits: Int) = "%.${digits}f".format(this)

fun Double.to2Digits() = String.format("%.2f", this).replace(",", ".").toDouble()

fun Double.toModMax(mod: Double, max: Double) : Double {
    val result = this / mod
    return if (result > max) max else result
}

fun Int.toModMax(mod: Double, max: Double) : Double {
    val result = this.toDouble() / mod
    return if (result > max) max else result
}

fun Long.toFormatDateTime() : String {
    return SimpleDateFormat("dd.MM.yy HH:mm:ss", Locale.getDefault()).format(Date(this))
}

fun User.lowDescriptor(): String {
    return descriptor().split(" :: ")[1]
}

fun catchToken(): List<String> {
    val file = File("token.dsc")
    if (!file.exists()) {
        file.createNewFile()
        //TODO Write token
    }
    val array = decrypt(file.readBytes(), DSC_PS).decodeToString().split("\n")
    return array
}

fun String.toBase64() : String {
    return Base64.getEncoder().encodeToString(toByteArray())
}

fun String.fromBase64(): String {
    val decodedBytes = Base64.getDecoder().decode(this)
    return String(decodedBytes)
}

fun formatInt(value: Int, items: Int) : String {
    var str = value.toString()
    while (str.length < items)
        str = "0$str"
    return str
}

fun User.toStringUID() = id.value.toString()

suspend fun reloadMatch(sqlData: SQLData, puuid: String, startIndex: Int) {
    val checkMatches = ArrayList<String>()
    LeagueMainObject.catchMatchID(puuid, startIndex,100).forEach ff@ { matchId ->
        checkMatches.add(matchId)
    }
    sqlData.getNewMatches(checkMatches).forEach {newMatch ->
        LeagueMainObject.catchMatch(newMatch)?.let { match ->
            sqlData.addMatch(match)
        }
    }
    checkMatches.clear()
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