package ru.descend.bot.savedObj

import com.google.gson.GsonBuilder
import dev.kord.common.entity.Snowflake
import dev.kord.core.cache.data.UserData
import dev.kord.core.entity.Guild
import dev.kord.core.entity.User
import kotlinx.serialization.Serializable
import ru.descend.bot.catchToken
import ru.descend.bot.data.Configuration
import ru.descend.bot.lolapi.EnumRegions
import ru.descend.bot.lolapi.LeagueApi
import ru.descend.bot.toStringUID
import java.io.File

@Serializable
data class DataBasic(
    var user: Person,
    var text: String = "",
    var date: Long = System.currentTimeMillis()
)

@Serializable
data class DataPSteal(
    var whoSteal: String,
    var fromWhomSteal: String,
    var heroSteal: String = "",

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

    fun updatePerson(new: Person){
        if (findForUUID(new.uid) == null)
            return

        listPersons.removeIf { it.uid == new.uid }
        listPersons.add(new)
    }

    fun findForUUID(uid: String) : Person? {
        return listPersons.find { it.uid == uid }
    }

    fun addPentaKill(uid: String, heroKey: String) {
        findForUUID(uid)?.pentaKills?.add(DataPKill(heroKey))
    }

    fun addPentaStill(user: User, uidWho: String = "0", uidFrom: String = "0", heroStill: String) {
        if (uidWho == uidFrom) return
        findForUUID(user.toStringUID())?.pentaStills?.add(DataPSteal(whoSteal = uidWho, fromWhomSteal = uidFrom, heroSteal = heroStill))
    }
}

@Serializable
class LeaguePerson {
    var id: String? = null
    var accountId: String? = null
    var puuid: String? = null
    var name: String? = null

    fun initialize(region: String, summonerName: String){
        val leagueApi = LeagueApi(catchToken()[1], region)
        leagueApi.leagueService.getBySummonerName(summonerName).execute().body()?.let {
            this.id = it.id
            this.accountId = it.accountId
            this.puuid = it.puuid
            this.name = it.name
        }
    }
}

@Serializable
class Person {
    var uid: String
    var name: String
    var discriminator: String
    var leaguePerson: LeaguePerson? = null

    var pentaKills: ArrayList<DataPKill> = ArrayList()
    var pentaStills: ArrayList<DataPSteal> = ArrayList()
    constructor(user: User) {
        this.uid = user.id.value.toString()
        this.name = user.username
        this.discriminator = user.discriminator
    }

    fun toUser(guild: Guild) : User {
        return User(UserData(Snowflake(uid.toLong()), name, discriminator), guild.kord)
    }
}

fun readPersonFile(guid: Guild): DataPerson {
    val fileDir = File("data")
    if (!fileDir.exists()) fileDir.mkdir()

    val file = File("${fileDir.path}/${guid.id.value}")
    if (!file.exists()) file.mkdir()
    val fileData = File(file.path + "/summoner_data.json")
    if (!fileData.exists()) {
        fileData.createNewFile()
        return DataPerson()
    }

    val readText = fileData.readText()
    if (readText.isEmpty() || readText.isBlank()) {
        fileData.createNewFile()
        return DataPerson()
    }

    return GsonBuilder().create().fromJson(fileData.readText(), DataPerson::class.java)
}

fun writePersonFile(guid: Guild, src: Any) {
    val fileDir = File("data")
    if (!fileDir.exists()) fileDir.mkdir()

    val file = File("${fileDir.path}/${guid.id.value}")
    if (!file.exists()) file.mkdir()
    val fileData = File(file.path + "/summoner_data.json")
    if (!fileData.exists()) {
        fileData.createNewFile()
    }

    fileData.writeText(GsonBuilder().create().toJson(src))
}