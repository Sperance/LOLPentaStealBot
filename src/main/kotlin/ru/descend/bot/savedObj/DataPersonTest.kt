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
import ru.descend.bot.getRandom
import ru.descend.bot.lolapi.LeagueApi
import java.io.File
import java.io.IOException
import java.net.ProtocolException

class DataPersonTest {

    private val leagueApi = LeagueApi("RGAPI-a3c4d742-818d-40c9-9ec3-01fe2c426757", LeagueApi.RU)

    @Test
    fun test_lol_api() {
        val exec = leagueApi.leagueService.getChampions()
        val execMod = exec.execute()
        val body = execMod.body()!!
        val namesAllHero = ArrayList<String>()
        body.data::class.java.declaredFields.forEach {
            it.isAccessible = true
            val curData = it.get(body.data)
            println("data: $curData")
            val nameField = curData::class.java.getDeclaredField("name")
            nameField.isAccessible = true
            namesAllHero.add(nameField.get(curData).toString())
        }
        println(namesAllHero.joinToString(separator = "\n"))
    }

    @Test
    fun test_okhttp_request() {
        val okHttpClient = OkHttpClient()

        val request = Request.Builder()
            .get()
            .addHeader("Origin", "https://developer.riotgames.com")
            .addHeader("X-Riot-Token", "RGAPI-a3c4d742-818d-40c9-9ec3-01fe2c426757")
            .url("http://ddragon.leagueoflegends.com/cdn/13.21.1/data/ru_RU/champion.json")
//            .url("https://ru.api.riotgames.com/lol/summoner/v4/summoners/by-name/%D0%90%D1%82%D0%BB%D0%B0%D0%BD%D1%82")
            .build()

        val res = okHttpClient.newCall(request).execute().body()!!.string()
        println(res)

//        okHttpClient.newCall(request).enqueue(object : Callback {
//            override fun onFailure(call: Call, e: IOException) {
//                println("Fail: ${e.message}")
//            }
//
//            override fun onResponse(call: Call, response: Response) {
//                response.use {
//                    if (!response.isSuccessful) {
//                        println("Fail Suc: ${response.code()} ${response.message()}")
//                        return
//                    }
//
//                    val mainBody = response.body()!!.string()
//                    println("Result: $mainBody")
//                }
//            }
//        })
    }

    @Test
    fun test_create_file(){
        val file = File("src/main/resources/data")
        if (!file.exists()) {
            file.mkdir()
        }
        val fileData = File(file.path + "/53453459345345_data.json")
        if (!fileData.exists()) {
            fileData.createNewFile()
        }
    }

    @Test
    fun test_randomize_int() {
        repeat(100){
            println(getRandom(5))
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