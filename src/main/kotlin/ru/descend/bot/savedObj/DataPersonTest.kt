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
import ru.descend.bot.firebase.CompleteResult
import ru.descend.bot.firebase.FirePKill
import ru.descend.bot.firebase.FirePerson
import ru.descend.bot.firebase.FirebaseService
import ru.descend.bot.getRandom
import ru.descend.bot.lolapi.LeagueApi
import ru.descend.bot.lolapi.LeagueMainObject
import ru.descend.bot.printLog
import ru.descend.bot.toStringUID
import java.io.FileInputStream

class DataPersonTest {
    @Test
    fun test_randomize_int() {
        repeat(100){
            println(getRandom(5))
        }
    }

    @Test
    fun test_format_number() {
        var str = ""

        val num1 = 4
        val num2 = 53
        val num3 = 0
        val num4 = 483
        val num5 = 11

        val strSize = 3
        val charS = "0"

        str += catchStr(num1, 4) + "/"
        str += catchStr(num2, 3) + "/"
        str += catchStr(num3, 2) + "/"
        str += catchStr(num4, 2) + "/"
        str += catchStr(num5, 2)

        println(str)
    }

    fun catchStr(value: Int, items: Int) : String {
        var str = value.toString()
        while (str.length < items)
            str = "0$str"
        return str
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
        lit@ ind.forEach {
            if (it == "2")
            println(it)
        }
        println("end")
    }

    @Test
    fun test_match_list() {
        println("RES: " + LeagueMainObject.catchMatchID("aiLA3E9wdeoRpw-b3In28yDLN8fz0KT25m2jGhc0eDLgqGzY-EbGoJpjVrXZsI9nU3zLa0Vg_Ip8Ag"))
    }

    @Test
    fun test_con() {
        val res = FirebaseService.checkDataForCollection(FirebaseService.firestore.collection("GUILDS"), "11417301481949962952")
        println("RES: $res")
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