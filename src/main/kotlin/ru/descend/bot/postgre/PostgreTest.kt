package ru.descend.bot.postgre

import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.junit.Test
import org.komapper.core.dsl.QueryDsl
import org.komapper.core.dsl.expression.WhereDeclaration
import org.komapper.core.dsl.metamodel.EntityMetamodel
import org.komapper.core.dsl.operator.count
import org.komapper.core.dsl.operator.or
import org.komapper.core.dsl.query.Query
import org.komapper.core.dsl.query.get
import ru.descend.bot.datas.Result
import ru.descend.bot.datas.Toppartisipants
import ru.descend.bot.enums.EnumMMRRank
import ru.descend.bot.datas.create
import ru.descend.bot.datas.getDataOne
import ru.descend.bot.datas.safeApiCall
import ru.descend.bot.postgre.r2dbc.model.KORDLOLs.Companion.tbl_kordlols
import ru.descend.bot.postgre.r2dbc.model.LOLs.Companion.tbl_lols
import ru.descend.bot.postgre.r2dbc.model.Matches
import ru.descend.bot.postgre.r2dbc.model.Matches.Companion.tbl_matches
import ru.descend.bot.printLog
import ru.descend.bot.datas.toLocalDate
import ru.descend.bot.enums.EnumMMRRank.entries
import ru.descend.bot.generateAIText
import ru.descend.bot.lolapi.LeagueMainObject
import ru.descend.bot.lolapi.LeagueMainObject.dragonService
import ru.descend.bot.lolapi.dto.InterfaceChampionBase
import ru.descend.bot.postgre.r2dbc.model.Heroes
import ru.descend.bot.postgre.r2dbc.model.Heroes.Companion.tbl_heroes
import ru.descend.bot.postgre.r2dbc.model.ParticipantsNew.Companion.tbl_participantsnew
import ru.descend.bot.to1Digits
import ru.gildor.coroutines.okhttp.await
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.Period
import java.util.Date
import java.util.HashMap

class PostgreTest {

    @Test
    fun test_mmrs(){
        printLog("MMR: ${getMMRRank(2236.3)}")
        printLog("MMR: ${getMMRRank(124.3)}")
        printLog("MMR: ${getMMRRank(1124.3)}")
        printLog("MMR: ${getMMRRank(0.0)}")
        printLog("MMR: ${getMMRRank(57.7)}")
    }

    fun getMMRRank(mmr: Double) : EnumMMRRank {
        return entries.sortedBy { it.minMMR }.firstOrNull { it.minMMR >= mmr } ?: entries.last()
    }

    private fun asyncLoadMatches(listChecked: List<String>, mainOrder: Boolean) {
        if (listChecked.size > 2) {
            val list1 = listChecked.subList(0, listChecked.size / 2).toList()
            println("list1: ${list1.joinToString()}")
            val list2 = listChecked.subList(listChecked.size / 2, listChecked.size).toList()
            println("list2: ${list2.joinToString()}")
            println()
        } else {
//            loadArray(listChecked, mainOrder)
        }
    }

    fun Long.betweenCurrent() : Period {
        return Period.between(this.toLocalDate(), Date().time.toLocalDate())
    }

    @Test
    fun test_list() {
        asyncLoadMatches(listOf("1", "2", "3", "4", "5", "6"), false)
        asyncLoadMatches(listOf("1", "2", "3", "4", "5"), false)
        asyncLoadMatches(listOf("1", "2", "3", "4"), false)
        asyncLoadMatches(listOf("1", "2", "3"), false)
    }

    @Test
    fun reset_heroes_table() {
        runBlocking {
            val versions = when (val res = safeApiCall { LeagueMainObject.dragonService.getVersions() }){
                is Result.Success -> res.data
                is Result.Error -> {
                    printLog("[reset_heroes_table] error: ${res.message}")
                    listOf()
                }
            }

            val champions = when (val res = safeApiCall { LeagueMainObject.dragonService.getChampions(versions.first(), "ru_RU") }){
                is Result.Success -> res.data
                is Result.Error -> {
                    printLog("[reset_heroes_table] error: ${res.message}")
                    throw IllegalAccessException("[reset_heroes_table] error: ${res.message}")
                }
            }

            champions.data::class.java.declaredFields.forEach {
                it.isAccessible = true
                val curData = it.get(champions.data) as InterfaceChampionBase
                val hero = Heroes(
                    nameEN = curData.id,
                    nameRU = curData.name,
                    key = curData.key,
                )
                hero.create(Heroes::key)
            }
        }
    }

    suspend fun <META : EntityMetamodel<Any, Any, META>> EntityMetamodel<*, *, *>.getSize(declaration: WhereDeclaration): Long {
        return R2DBC.runQuery { QueryDsl.from(this@getSize as META).where(declaration).select(count()) }?:0L
    }

    @Test
    fun test_generics2() {
        runBlocking {
            val data = tbl_lols.getSize { tbl_lols.id greaterEq 0 }
            println(data)
        }
    }

    @Test
    fun test_with_transactions() {
        runBlocking {
            var hero = Heroes().getDataOne({ tbl_heroes.id eq 1 })!!
            println(hero)

            hero.otherNames = ""
            hero.nameRU = "Атрокс"

            hero = Heroes().getDataOne({ tbl_heroes.id eq 1 })!!
            println(hero)
        }
    }

    @Test
    fun test_without_transactions() {
        runBlocking {
//            var hero = Heroes().getDataOne({ tbl_heroes.id eq 1 })!!
//            println(hero)
//
//            hero.otherNames = "12345"
//            hero.updateWithoutT()
//
//            hero = Heroes().getDataOne({ tbl_heroes.id eq 1 })!!
//            println(hero)
        }
    }

    @Test
    fun test_digits_double() {
        printLog((523.523).to1Digits())
        printLog((523.53).to1Digits())
        printLog((523.3).to1Digits())
    }

    fun isNeedSetLowerYear(dateValue: String) : Boolean {
        val valueDay = dateValue.substring(0..1).toInt()
        val valueMonth = dateValue.substring(2..3).toInt()
        val curDate = LocalDate.of(2000, valueMonth, valueDay)
        val curSysDate = LocalDate.of(2000, LocalDate.now().monthValue, LocalDate.now().dayOfMonth)
        if (curDate < curSysDate) return false
        if (curDate > curSysDate) return true
        if (curDate == curSysDate) return true
        return false
    }

    data class DataChampionWinstreak(
        val championId: Int,
        var championGames: Int,
        var championWins: Int,
        var championKDA: Double
    )

    @Test
    fun test_remove_match() {
                var resultAra = arrayListOf("RU_484835441", "RU_484835442", "RU_484835443")
                val dataAra = resultAra.joinToString(prefix = "{", postfix = "}")
                println(dataAra)
                val sql = "SELECT remove_matches('$dataAra'::character varying[])"
                QueryDsl.fromTemplate(sql).select {
                    val data = it.get<Array<String>>(0)
                    resultAra.removeAll(data!!.toSet())
                    println("RES: ${resultAra.joinToString()}")
                }
    }

    @Test
    fun test_parse_json() {
        runBlocking {
            val champions = when (val res = safeApiCall { dragonService.getChampions("14.14.1", "ru_RU") }){
                is Result.Success -> res.data
                is Result.Error -> {
                    printLog("[catchHeroNames] error: ${res.message}")
                    throw IllegalAccessException("[catchHeroNames] error: ${res.message}")
                }
            }
            val result = Gson().fromJson(Gson().toJson(champions), ChampionsDTOsample::class.java)
            var count = 0
            result.data.forEach { (_, any2) ->
                count++
                println("any3: ${Gson().fromJson(Gson().toJson(any2), InterfaceChampionBase::class.java)}")
            }
            println("Count: $count")
        }
    }

    data class ChampionsDTOsample(
        val type: String,
        val format: String,
        val version: String,
        val data: HashMap<Any, Any>,
    )

    @Test
    fun test_api_2() {

        //@GET("http://ddragon.leagueoflegends.com/cdn/{version}/data/{locale}/champion.json")

        val url = URL("http://ddragon.leagueoflegends.com/cdn/14.14.1/data/ru_RU/champion.json")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("Accept", "application/json")
        connection.doOutput = true

        val reader = BufferedReader(InputStreamReader(connection.inputStream, "UTF-8"))
        var line: String?
        var response = ""

        while (reader.readLine().also { line = it } != null) {
            response += line
        }

        reader.close()
        connection.disconnect()

//        println("url: ${connection.url}")
//        println("requestMethod: ${connection.requestMethod}")
//        println("responseCode: ${connection.responseCode}")
//        println("responseMessage: ${connection.responseMessage}")
        println(response)
    }

    @Test
    fun test_digits(){
        println((123.4).to1Digits())
        println((-123.4).to1Digits())
        println((2.4).to1Digits())
        println((-2.4).to1Digits())
        println((-2.0).to1Digits())
        println((2.0).to1Digits())
    }

    @Test
    fun test_proxy_older() {
        runBlocking {
//            val url = "https://api.openai.com/v1/chat/completions"
            val url = "https://api.mistral.ai/chat/completions"
//            val url = "https://descend-oai-proxy5.hf.space/proxy/openai/v1/chat/completions"
            val requestText = "Hello? how are you?"
            val JSON = "application/json; charset=utf-8".toMediaType()
            val body = RequestBody.create(JSON, "{\n" +
                    "        \"model\": \"gpt-3.5-turbo\",\n" +
                    "        \"messages\": [{\"role\": \"user\", \"content\": \"$requestText\"}]\n" +
                    "    }")
            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()
            val response = OkHttpClient().newCall(request).await()
            println("code: ${response.code}")
            println("message: ${response.message}")
            println("result: ${response.body?.string()}")
        }
    }

    /**
     * https://infostart.ru/1c/articles/1978921/?ysclid=lxuhv65xoq53314357&ID=1978921
     *
     * https://huggingface.co/spaces/mangelaav/oai-proxy
     */
    @Test
    fun test_free_api_proxy() {
        runBlocking {
//            val url = "https://descend-oai-proxy-v1.hf.space/proxy/openai/v1/chat"
            val url = "https://descend-oai-proxy-v1.hf.space/proxy/openai/v1/chat/completions"

            val requestText = "Hello? how are you?"
            val JSON = "application/json; charset=utf-8".toMediaType()
            val body = RequestBody.create(JSON, "{\n" +
                    "    \"model\": \"gpt-3.5-turbo\",\n" +
                    "    \"messages\": [{\"role\": \"user\", \"content\": \"$requestText\"}]\n" +
                    "  }")

            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            val response = OkHttpClient().newCall(request).await()

            println("code: ${response.code}")
            println("message: ${response.message}")
            println("result: ${response.body?.string()}")
        }
    }

    @Test
    fun test_AI_context() {
        runBlocking {
            val generatedText = generateAIText("Напиши прикольный факт про игру League of Legends")
            val resultedText = "**Рубрика: интересные факты**\n\n$generatedText"
            println(resultedText)
        }
    }

    @Test
    fun test_match_region() {
        val match1 = "RU_21312"
        val match2 = "EUW1_341535"
        println(match1.substringBefore("_"))
        println(match2.substringBefore("_"))
    }

    @Test
    fun testPart() {
        runBlocking {
            val query = QueryDsl
                .from(tbl_participantsnew)
                .leftJoin(tbl_matches) { tbl_matches.id eq tbl_participantsnew.match_id }
                .innerJoin(tbl_kordlols) { tbl_kordlols.LOL_id eq tbl_participantsnew.LOLperson_id }
                .where { tbl_matches.matchMode.inList(listOf("ARAM", "CLASSIC")) }
                .orderBy(tbl_participantsnew.id)
                .selectAsEntity(tbl_participantsnew)

            val statClass = Toppartisipants()
            val result = R2DBC.runQuery { query }//.sortedBy { it.id }
            result.forEach {
                statClass.calculateField(it, "Нанесено урона чемпионам", it.totalDamageDealtToChampions.toDouble())
            }

            var resultText = ""
            statClass.getResults().forEach {
                resultText += "* $it\n"
            }
            printLog("size: ${resultText.length}")
        }
    }

    @Test
    fun test_duplicates() {
        runBlocking {
            var newPart = Matches().getDataOne({ tbl_matches.matchId eq "RU_493104216" })!!
            println(Gson().toJson(newPart))
        }
    }

    @Test
    fun test_updates() {
        runBlocking {
            val query: Query<Long> = QueryDsl.update(tbl_heroes).set {
                tbl_heroes.otherNames eq ""
            }.where {
                tbl_heroes.id less 10
            }
            R2DBC.runQuery { query }
        }
    }

    @Test
    fun test_correct() {
        runBlocking {

            val whereSurrender: WhereDeclaration = {
                tbl_matches.surrender eq true
                tbl_participantsnew.needCalcStats eq true
            }
            val whereAborted: WhereDeclaration = {
                tbl_matches.aborted eq true
                tbl_participantsnew.needCalcStats eq true
            }
            val whereBots: WhereDeclaration = {
                tbl_matches.bots eq true
                tbl_participantsnew.needCalcStats eq true
            }
            val sumWhere: WhereDeclaration = whereSurrender.or(whereAborted).or(whereBots)

            var size = 0L
            val query = QueryDsl
                .from(tbl_participantsnew)
                .leftJoin(tbl_matches) { tbl_matches.id eq tbl_participantsnew.match_id }
                .where(sumWhere)
                .select(tbl_participantsnew.id, tbl_matches.matchDateEnd)
            val result = R2DBC.runQuery { query }

            result.forEach {
                if (it.first == null) return@forEach

                val newDate = it.second?:0L
                val queryUpdate: Query<Long> = QueryDsl.update(tbl_participantsnew).set {
                    tbl_participantsnew.matchDateEnd eq newDate
                    tbl_participantsnew.needCalcStats eq false
                }.where {
                    tbl_participantsnew.id eq it.first
                }
                R2DBC.runQuery { queryUpdate }

                println("id: ${it.first} date: $newDate (updated ${++size} of ${result.size})")
            }
        }
    }

    @Test
    fun test_birthday_parse() {
        val dateValue = "05041900_2024"

        val valueDay = dateValue.substring(0..1).toInt()
        val valueMonth = dateValue.substring(2..3).toInt()
        val valueYear = dateValue.substring(4..7).toInt()
        printLog("d$valueDay m$valueMonth y$valueYear")

        val curDate = LocalDate.of(2000, valueMonth, valueDay)
        printLog(curDate.dayOfMonth)
        printLog(curDate.monthValue)
        printLog(curDate.year)

        val curSysDate = LocalDate.of(2000, LocalDate.now().monthValue, LocalDate.now().dayOfMonth)
        if (curDate < curSysDate) printLog("low")
        if (curDate > curSysDate) printLog("great")
        if (curDate == curSysDate) printLog("eq")
    }

    @Test
    fun checkMatchContains() {
        EnumMMRRank.entries.forEach {
            printLog("${it.nameRank} - ${it.ordinal} - ${((it.ordinal / 10.0) * 2.0) + 1.0}")
        }
    }
}