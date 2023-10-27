package ru.descend.bot.savedObj

import com.google.gson.GsonBuilder
import dev.kord.core.entity.Guild
import dev.kord.core.entity.User
import kotlinx.serialization.Serializable
import java.io.File

@Serializable
class CalendarData {
    var date: Long = -1L
    init {
        this.date = System.currentTimeMillis()
    }
}

@Serializable
class DataPerson {

    var listPersons = ArrayList<Person>()

    fun addPersons(person: Person){
        if (findForUUID(person.uid) != null)
            return
        listPersons.add(person)
    }

    fun findForUUID(uid: String) : Person? {
        return listPersons.find { it.uid == uid }
    }

    fun addPentaKill(uid: String) {
        findForUUID(uid)?.pentaKills?.add(CalendarData())
    }

    fun addPentaStill(uid: String, uidFrom: String = "0") {
        findForUUID(uid)?.pentaStills?.add(Pair(uidFrom, CalendarData()))
    }
}

@Serializable
class Person {
    var uid: String
    var name: String
    var pentaKills: ArrayList<CalendarData> = ArrayList()
    var pentaStills: ArrayList<Pair<String, CalendarData>> = ArrayList()
    constructor(user: User) {
        this.uid = user.id.value.toString()
        this.name = user.username
    }
}

fun readDataFile(guid: Guild): DataPerson {

    val file = File("src/main/resources/data")
    if (!file.exists()) {
        file.mkdir()
    }
    val fileData = File(file.path + "/${guid.id.value}_data.json")
    if (!fileData.exists()) {
        fileData.createNewFile()
        return DataPerson()
    }

    val gson = GsonBuilder().create()
    return gson.fromJson(fileData.readText(), DataPerson::class.java)
}

fun writeDataFile(guid: Guild, src: Any) {

    val file = File("src/main/resources/data")
    if (!file.exists()) {
        file.mkdir()
    }
    val fileData = File(file.path + "/${guid.id.value}_data.json")
    if (!fileData.exists()) {
        fileData.createNewFile()
    }

    val gson = GsonBuilder().create()
    val data = gson.toJson(src)
    fileData.writeText(data)
}