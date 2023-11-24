package ru.descend.bot.savedObj

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.cloud.FirestoreClient
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Test
import ru.descend.bot.catchToken
import ru.descend.bot.firebase.FirePKill
import ru.descend.bot.firebase.FirePerson
import ru.descend.bot.firebase.FirebaseService
import ru.descend.bot.getRandom
import ru.descend.bot.lolapi.LeagueApi
import ru.descend.bot.lolapi.LeagueMainObject
import java.io.FileInputStream

class DataPersonTest {
    @Test
    fun test_randomize_int() {
        repeat(100){
            println(getRandom(5))
        }
    }

    @Test
    fun test_connect_rito(){
        val leagueApi = LeagueApi(catchToken()[1], LeagueApi.RU)
        val ecex = leagueApi.leagueService.getBySummonerName("Атлант").execute()
        println("exec: " + ecex.raw())
        val response = ecex.body()
        println(response?.id + " " + response?.name + " " + response?.accountId + " " + response?.puuid + " " + response?.summonerLevel)
    }

    @Test
    fun test_firebase(){
        val leagueApi = LeagueApi(catchToken()[1], LeagueApi.RU)
        println(leagueApi.leagueService.getMatchIDByPUUID("ML88UCuH67sUxzc7p9-E6UsF-s_AP6rlcXUeJ4O1jix-xUXOwCti9nHM7EhASAgEvkqPDdFGkh8Msg").execute().body())
    }

    @Test
    fun test_loop() {
        val ind = ArrayList<String>()
        ind.add("1")
        ind.add("2")
        ind.add("3")
        ind.add("4")
        ind.add("5")
        ind.forEach lit@ {
            if (it == "2") return@lit
            println(it)
        }
    }

//    @Test
//    fun writeDataTest() {
//        val data = DataPerson()
//        data.addPersons(Person(123123123u, "Sample name"))
//        data.addPersons(Person(1734573123u, "sadfsfd name").apply {
//            this.pentaKills = 42
//            this.pentaStills["41410924"] = 12
//            this.pentaStills["64357734"] = 2
//        })
//        data.addPersons(Person(957656723u, "Descend"))
//        data.addPersons(Person(53245346u, "Descend"))
//        data.addPersons(Person(77547654656u, "Descend"))
//        writeDataFile(data)
//    }
//
//    @Test
//    fun readDataTest() {
//        val obj = readDataFile()
//        println("Soze: ${obj.listPersons.size}")
//        println(obj)
//
//        println("Find: ${obj.findForUUID("1734573123")?.pentaStills}")
//    }
//
//    @Test
//    fun testAddValues() {
//        val obj = readDataFile()
//
//        println(obj.listPersons.joinToString { it.uid + " " + it.name })
//
//        obj.addPentaStill("957656723")
//        obj.addPentaStill("957656723")
//        obj.addPentaStill("957656723", "53245346")
//        obj.addPentaStill("957656723", "53245346")
//        writeDataFile(obj)
//    }
}