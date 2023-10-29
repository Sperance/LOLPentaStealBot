package ru.descend.bot.savedObj

import com.google.gson.GsonBuilder
import dev.kord.core.entity.Guild
import dev.kord.core.entity.User
import kotlinx.serialization.Serializable
import ru.descend.bot.data.Configuration
import java.io.File

@Serializable
data class DataPSteal(
    var whoSteal: String,
    var fromWhomSteal: String,
    var heroWhoSteal: String = "",
    var heroWhomSteal: String = "",

    var date: Long = System.currentTimeMillis()
)

@Serializable
data class DataPKill(
    var heroKey: String,

    var date: Long = System.currentTimeMillis()
)

@Serializable
class DataPerson {

    var listPersons = ArrayList<Person>()

    fun addPersons(person: Person){

        if (person.uid == Configuration.botCurrentId.value.toString())
            return

        if (findForUUID(person.uid) != null)
            return

        listPersons.add(person)
    }

    fun findForUUID(uid: String) : Person? {
        return listPersons.find { it.uid == uid }
    }

    fun addPentaKill(uid: String, heroKey: String) {
        findForUUID(uid)?.pentaKills?.add(DataPKill(heroKey))
    }

    fun addPentaStill(uid: String = "0", uidFrom: String = "0") {
        if (uid == uidFrom) return
        val userUid = if (uid == "0") uidFrom else uid
        findForUUID(userUid)?.pentaStills?.add(DataPSteal(whoSteal = uid, fromWhomSteal = uidFrom))
    }
}

@Serializable
class Person {
    var uid: String
    var name: String
    var pentaKills: ArrayList<DataPKill> = ArrayList()
    var pentaStills: ArrayList<DataPSteal> = ArrayList()
    constructor(user: User) {
        this.uid = user.id.value.toString()
        this.name = user.username
    }
}

fun readDataFile(guid: Guild): DataPerson {

    val file = File("src/main/resources/data/${guid.id.value}")
    if (!file.exists()) {
        file.mkdir()
    }
    val fileData = File(file.path + "/summoner_data.json")
    if (!fileData.exists()) {
        fileData.createNewFile()
        return DataPerson()
    }

    val gson = GsonBuilder().create()
    return gson.fromJson(fileData.readText(), DataPerson::class.java)
}

fun writeDataFile(guid: Guild, src: Any) {

    val file = File("src/main/resources/data/${guid.id.value}")
    if (!file.exists()) {
        file.mkdir()
    }
    val fileData = File(file.path + "/summoner_data.json")
    if (!fileData.exists()) {
        fileData.createNewFile()
    }

    val gson = GsonBuilder().create()
    val data = gson.toJson(src)
    fileData.writeText(data)
}