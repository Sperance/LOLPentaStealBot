package ru.descend.bot.savedObj

import Entity
import column
import databases.Database
import org.junit.jupiter.api.Test
import ru.descend.bot.catchToken
import ru.descend.bot.firebase.FirebaseService
import ru.descend.bot.getRandom
import ru.descend.bot.lolapi.LeagueApi
import ru.descend.bot.printLog
import table

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

    data class UserPost(
        override var id: Int = 0,
        var username: String = "",
        var age: Int? = 18,
    ) : Entity() {
        override fun toString(): String {
            return "UserPost(id=$id, username='$username', age=$age)"
        }
    }

    val userTable = table<UserPost, Database> {
        column(UserPost::username).unique()
        check(UserPost::age) { it greaterEq 18 }

        defaultEntities { listOf(
            UserPost(username = "Mike", age = 21),
            UserPost(username = "Sue", age = 35),
            UserPost(username = "Bill", age = 27),
        ) }
    }

    @Test
    fun test_date() {
        val curDate = getStrongDate(System.currentTimeMillis())
        printLog("Hour: ${curDate.hours}")
        when (curDate.hours){
            in 1..11 -> {
                printLog("Hour: ${curDate.hours} - continue")
            }
        }
    }

    @Test
    fun test_con() {
        val res = FirebaseService.checkDataForCollection(FirebaseService.firestore.collection("GUILDS"), "11417301481949962952")
        println("RES: $res")
    }
}