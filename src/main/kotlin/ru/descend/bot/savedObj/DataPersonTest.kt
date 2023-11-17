package ru.descend.bot.savedObj

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.junit.jupiter.api.Test
import ru.descend.bot.DSC_PS
import ru.descend.bot.catchToken
import ru.descend.bot.decrypt
import ru.descend.bot.encrypt
import ru.descend.bot.getRandom
import ru.descend.bot.lolapi.LeagueApi
import java.io.File
import java.io.IOException
import java.net.ProtocolException

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