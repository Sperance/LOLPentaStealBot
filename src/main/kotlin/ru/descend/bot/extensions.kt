package ru.descend.bot

import dev.kord.common.entity.Permission
import dev.kord.core.entity.Guild
import dev.kord.core.entity.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import me.jakejmattson.discordkt.extensions.descriptor
import ru.descend.bot.lolapi.LeagueMainObject
import ru.descend.bot.postgre.PostgreSQL
import java.io.File
import java.text.SimpleDateFormat
import java.util.Base64
import java.util.Date
import java.util.Locale
import java.util.Random
import kotlin.math.pow

fun printLog(message: Any){
    val curDTime = System.currentTimeMillis().toFormatDateTime()
    println("[$curDTime] $message")
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

suspend fun <T> Flow<T>.asList(): ArrayList<T> {
    val emptyList = ArrayList<T>()
    collect { emptyList.add(it) }
    return emptyList
}

fun formatInt(value: Long, items: Int) : String {
    var str = value.toString()
    while (str.length < items)
        str = "0$str"
    return str
}

fun formatInt(value: Int, items: Int) : String {
    var str = value.toString()
    while (str.length < items)
        str = "0$str"
    return str
}

fun getRandom(): Random {
    RAND_INT_SEED++
    return Random(System.currentTimeMillis() + RAND_INT_SEED)
}

private var RAND_INT_SEED = 1

fun getRandom(maxPos: Int) : Int {
    return getRandom().nextInt(maxPos)
}

fun User.toStringUID() = id.value.toString()

suspend fun reloadMatch(guild: Guild, puuid: String, startIndex: Int) {
    LeagueMainObject.catchMatchID(puuid, startIndex,100).forEach mch@ { matchId ->
        if (sqlCurrentMatches[guild.id.value.toString()]!!.find { mch -> mch.matchId == matchId } == null) {
            LeagueMainObject.catchMatch(matchId)?.let { match ->
                PostgreSQL.getGuild(guild).addMatch(match)
            }
        }
    }
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