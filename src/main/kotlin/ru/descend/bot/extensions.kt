package ru.descend.bot

import com.google.gson.GsonBuilder
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
import me.jakejmattson.discordkt.util.descriptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import ru.descend.bot.postgre.SQLData_R2DBC
import ru.descend.bot.postgre.openapi.AIResponse
import ru.descend.bot.savedObj.DSC_PS
import ru.descend.bot.savedObj.decrypt
import ru.descend.bot.savedObj.getStrongDate
import ru.gildor.coroutines.okhttp.await
import java.io.File
import java.math.RoundingMode
import java.nio.file.Files
import java.text.DecimalFormat
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

suspend fun SQLData_R2DBC?.sendMessage(messageId: String, message: String, afterLaunchBody: (() -> Unit)? = null) {
    if (this == null) return
    if (messageId.isEmpty()) return
    if (message.isEmpty()) return
    launch {
        try {
            val channelText = guild.getChannelOf<TextChannel>(Snowflake(messageId))
            channelText.createMessage {
                content = message
            }
        }catch (e: Exception) {
            printLog(guild, "Not sended message $message for channel $messageId. Error: ${e.message}")
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

fun <T, E> Map<T, E>.toStringMap() : String {
    var block = ""
    forEach { (t, e) ->
        block += "{$t:$e}"
    }
    return block
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

fun Double.to1Digits() = String.format("%.1f", this).replace(",", ".").toDouble()

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

val File.size get() = if (!exists()) 0.0 else length().toDouble()
val File.sizeInKb get() = size / 1024
val File.sizeInMb get() = sizeInKb / 1024
val File.sizeInGb get() = sizeInMb / 1024
val File.sizeInTb get() = sizeInGb / 1024

fun catchToken(): List<String> {
    val file = File("token.dsc")
    if (!file.exists()) {
        file.createNewFile()
        //TODO Write token 1 - dicsord/2 - LOL/3 - Gemini
    }
    return decrypt(file.readBytes(), DSC_PS).decodeToString().split("\n")
}

suspend fun generateAIText(requestText: String) : String {
    val url = "https://api.proxyapi.ru/openai/v1/chat/completions"
    val JSON = "application/json; charset=utf-8".toMediaType()
    val body = RequestBody.create(JSON, "{\n" +
            "        \"model\": \"gpt-3.5-turbo-1106\",\n" +
            "        \"messages\": [{\"role\": \"user\", \"content\": \"$requestText\"}]\n" +
            "    }")
    val request = Request.Builder()
        .addHeader("Authorization", "Bearer sk-LT7VD2dmZoQtR0VXftSq4YpXnkS8xcxW")
        .url(url)
        .post(body)
        .build()

    return try {
        val response = OkHttpClient().newCall(request).await()
        val resultString = response.body?.string()
        val forecast = GsonBuilder().create().fromJson(resultString, AIResponse::class.java)
        forecast.choices.first().message.content
    } catch (e: Exception) {
        ""
    }
}

fun writeLog(text: String?) {
    val pathFile = File("logs")
    if (!pathFile.exists()) Files.createDirectory(pathFile.toPath())
    val curDateText = Date().getStrongDate().date
    var logFile = File(pathFile.path, "log-$curDateText.txt")
    if (!logFile.exists()) logFile.createNewFile()

    if (logFile.sizeInMb > 2.0) {
        logFile = File(pathFile.path, "log-$curDateText-${pathFile.listFiles()?.size}.txt")
    }

    val curDTime = System.currentTimeMillis().toFormatDateTime()
    logFile.appendText("[$curDTime] $text\n")
}

fun String.toBase64() : String {
    return Base64.getEncoder().encodeToString(toByteArray())
}

fun String.fromBase64(): String {
    val decodedBytes = Base64.getDecoder().decode(this)
    return String(decodedBytes)
}

fun Double?.toFormat(items: Int) : String {
    if (this == null) return "-0.0"
    var pattern = ""
    for (i in 1..items) {
        pattern += "#"
    }
    val df = DecimalFormat("#.$pattern")
    df.roundingMode = RoundingMode.DOWN
    return df.format(this)
}

fun formatInt(value: Int?, items: Int) : String {
    var str = value.toString()
    while (str.length < items)
        str = "0$str"
    return str
}

fun String?.toMaxSymbols(symbols: Int, addIfCatched: String = "") : String {
    if (this == null) return ""
    if (this.length <= symbols) return this
    return this.substring(0, symbols) + addIfCatched
}

fun User.toStringUID() = id.value.toString()

//suspend fun reloadMatch(sqlData: SQLData_R2DBC, puuid: String, startIndex: Int) {
//    val checkMatches = ArrayList<String>()
//    LeagueMainObject.catchMatchID(sqlData, puuid, startIndex,100).forEach ff@ { matchId ->
//        checkMatches.add(matchId)
//    }
//    sqlData.getNewMatches(checkMatches).forEach {newMatch ->
//        LeagueMainObject.catchMatch(sqlData, newMatch)?.let { match ->
//            sqlData.addMatch(match)
//        }
//    }
//    checkMatches.clear()
//}

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

fun printMemoryUsage(addText: String = "") {
    val mb = 1024 * 1024
    val memValue = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / mb
    val heapSize = Runtime.getRuntime().totalMemory() / mb
    printLog("[Memory Usage::$memValue MB Size::$heapSize MB All::${heapSize + memValue} MB (pid ${ProcessHandle.current().pid()})] $addText")
}