package ru.descend.bot.savedObj

import com.google.gson.GsonBuilder
import dev.kord.core.entity.Guild
import kotlinx.serialization.Serializable
import java.io.File

@Serializable
class DataFile {

    var botChannelId: String? = null
    var messageId: String? = null
}

fun readDataFile(guid: Guild): DataFile {
    val fileDir = File("data")
    if (!fileDir.exists()) fileDir.mkdir()

    val file = File("${fileDir.path}/${guid.id.value}")
    if (!file.exists()) file.mkdir()
    val fileData = File(file.path + "/guild_data.json")
    if (!fileData.exists()) {
        fileData.createNewFile()
        return DataFile()
    }

    return GsonBuilder().create().fromJson(fileData.readText(), DataFile::class.java) ?: return DataFile()
}

fun writeDataFile(guid: Guild, src: Any) {
    val fileDir = File("data")
    if (!fileDir.exists()) fileDir.mkdir()

    val file = File("${fileDir.path}/${guid.id.value}")
    if (!file.exists()) file.mkdir()
    val fileData = File(file.path + "/guild_data.json")
    if (!fileData.exists()) {
        fileData.createNewFile()
    }

    fileData.writeText(GsonBuilder().create().toJson(src))
}