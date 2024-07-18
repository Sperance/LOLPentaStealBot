package ru.descend.bot.postgre

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dev.kord.core.behavior.channel.createMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import me.jakejmattson.discordkt.util.fullName
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.junit.Test
import org.komapper.core.dsl.QueryDsl
import org.komapper.core.dsl.expression.SortExpression
import org.komapper.core.dsl.expression.WhereDeclaration
import org.komapper.core.dsl.metamodel.EntityMetamodel
import org.komapper.core.dsl.metamodel.PropertyMetamodel
import org.komapper.core.dsl.operator.count
import org.komapper.core.dsl.operator.desc
import org.komapper.core.dsl.operator.max
import org.komapper.core.dsl.query.Query
import org.komapper.core.dsl.query.bind
import org.komapper.core.dsl.query.double
import org.komapper.core.dsl.query.firstOrNull
import org.komapper.core.dsl.query.get
import org.komapper.core.dsl.query.int
import org.komapper.core.dsl.query.single
import org.komapper.core.dsl.query.singleOrNull
import org.komapper.core.dsl.query.string
import org.komapper.core.dsl.visitor.QueryVisitor
import ru.descend.bot.EnumMeasures
import ru.descend.bot.datas.AESUtils
import ru.descend.bot.datas.CoreResult
import ru.descend.bot.datas.DataStatRate
import ru.descend.bot.datas.Result
import ru.descend.bot.datas.Toppartisipants
import ru.descend.bot.datas.addBatch
import ru.descend.bot.enums.EnumMMRRank
import ru.descend.bot.postgre.openapi.AIResponse
import ru.descend.bot.datas.create
import ru.descend.bot.datas.getData
import ru.descend.bot.datas.getDataOne
import ru.descend.bot.datas.getDataOneWithoutTransaction
import ru.descend.bot.datas.getDataWithoutTransaction
import ru.descend.bot.datas.getInstanceClassForTbl
import ru.descend.bot.datas.getSize
import ru.descend.bot.datas.getStrongDate
import ru.descend.bot.datas.safeApiCall
import ru.descend.bot.postgre.r2dbc.model.KORDLOLs.Companion.tbl_kordlols
import ru.descend.bot.postgre.r2dbc.model.LOLs.Companion.tbl_lols
import ru.descend.bot.postgre.r2dbc.model.MMRs
import ru.descend.bot.postgre.r2dbc.model.Matches
import ru.descend.bot.postgre.r2dbc.model.Matches.Companion.tbl_matches
import ru.descend.bot.datas.update
import ru.descend.bot.printLog
import ru.descend.bot.datas.toDate
import ru.descend.bot.datas.toLocalDate
import ru.descend.bot.generateAIText
import ru.descend.bot.lolapi.LeagueMainObject
import ru.descend.bot.lolapi.dto.InterfaceChampionBase
import ru.descend.bot.lowDescriptor
import ru.descend.bot.measureBlock
import ru.descend.bot.postgre.calculating.Calc_MMR
import ru.descend.bot.postgre.r2dbc.model.Guilds
import ru.descend.bot.postgre.r2dbc.model.Guilds.Companion.tbl_guilds
import ru.descend.bot.postgre.r2dbc.model.Heroes
import ru.descend.bot.postgre.r2dbc.model.Heroes.Companion.tbl_heroes
import ru.descend.bot.postgre.r2dbc.model.KORDLOLs
import ru.descend.bot.postgre.r2dbc.model.KORDs
import ru.descend.bot.postgre.r2dbc.model.KORDs.Companion.tbl_kords
import ru.descend.bot.postgre.r2dbc.model.LOLs
import ru.descend.bot.postgre.r2dbc.model.MMRs.Companion.tbl_mmrs
import ru.descend.bot.postgre.r2dbc.model.ParticipantsNew
import ru.descend.bot.postgre.r2dbc.model.ParticipantsNew.Companion.tbl_participantsnew
import ru.descend.bot.to1Digits
import ru.descend.bot.toBase64
import ru.descend.bot.toDate
import ru.descend.bot.toFormat
import ru.descend.bot.toFormatDate
import ru.descend.bot.toFormatDateTime
import ru.descend.bot.toStringUID
import ru.descend.bot.toTextFields
import ru.gildor.coroutines.okhttp.await
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.Period
import java.time.temporal.TemporalUnit
import java.util.Calendar
import java.util.Date
import java.util.HashMap
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.reflect.KMutableProperty1
import kotlin.system.measureTimeMillis
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds

class PostgreTest {

    @Test
    fun test_mmrs(){
        printLog("MMR: ${EnumMMRRank.getMMRRank(42.0)}")
        printLog("MMR: ${EnumMMRRank.getMMRRank(420.0)}")
        printLog("MMR: ${EnumMMRRank.getMMRRank(0.0)}")
        printLog("MMR: ${EnumMMRRank.getMMRRank(89.5)}")
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
    fun test_api_2() {
        val url = URL("https://descend-oai-proxy3.hf.space")
//        val url = URL("https://descend-oai-proxy3.hf.space/v1/chat/completions")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("Accept", "application/json")
        connection.doOutput = true

        val writer = OutputStreamWriter(connection.outputStream, "UTF-8")
        writer.write("{model=\"gpt-3.5-turbo\",\n" +
                "    messages=[{\"role\": \"user\", \"content\": \"Hello\"}],}")
        writer.close()

        val reader = BufferedReader(InputStreamReader(connection.inputStream, "UTF-8"))
        var line: String?
        var response = ""

        while (reader.readLine().also { line = it } != null) {
            response += line
        }

        reader.close()
        connection.disconnect()

        println("url: ${connection.url}")
        println("requestMethod: ${connection.requestMethod}")
        println("responseCode: ${connection.responseCode}")
        println("responseMessage: ${connection.responseMessage}")
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
    fun test_data_double() {
        val value = 2.45
        println(value.fromDoublePerc(2.0))
        println((value / 2.0).to1Digits())

        println(value.fromDoublePerc(3.0))
        println((value / 3.0).to1Digits())

        println(value.fromDoublePerc(1.6))
        println((value / 1.6).to1Digits())

    }

    private fun Double.fromDoublePerc(stock: Double): Double {
        return when ((this / stock) * 100.0) {
            in Double.MIN_VALUE..20.0 -> 0.2
            in 20.0..40.0 -> 0.4
            in 40.0..60.0 -> 0.6
            in 60.0..80.0 -> 0.8
            in 80.0..100.0 -> 1.0
            in 100.0..120.0 -> 1.2
            in 120.0..140.0 -> 1.4
            in 140.0..160.0 -> 1.6
            in 160.0..180.0 -> 1.8
            in 180.0..200.0 -> 2.0
            in 200.0..220.0 -> 2.2
            in 220.0..240.0 -> 2.4
            in 240.0..260.0 -> 2.6
            in 260.0..280.0 -> 2.8
            in 280.0..Double.MAX_VALUE -> 3.0
            else -> 0.0
        }
    }

    @Test
    fun test_get_data() {
        runBlocking {
            measureBlock(EnumMeasures.BLOCK, "1") {
                println(ParticipantsNew().getDataWithoutTransaction({ tbl_participantsnew.match_id eq 754421 }))
            }
            measureBlock(EnumMeasures.BLOCK, "2") {
                println(ParticipantsNew().getDataOneWithoutTransaction({ tbl_participantsnew.match_id eq 754421 }))
            }
            measureBlock(EnumMeasures.BLOCK, "3") {
                println(R2DBC.runQuery { QueryDsl.from(tbl_participantsnew).where { tbl_participantsnew.match_id eq 754421 } })
            }
        }
    }

    @Test
    fun tset_Asda(){
        runBlocking {
//            val data = R2DBC.runQuery { QueryDsl.from(tbl_participantsnew).where { tbl_participantsnew.kills5.isNotNull() }.selectAsEntity(tbl_participantsnew, max(tbl_participantsnew.kills5)).singleOrNull() }
//            println(data)

            val sql = "SELECT LOLperson_id, MAX(kills5) as ks FROM tbl_participants_new GROUP BY LOLperson_id ORDER BY ks DESC LIMIT 1"
            R2DBC.runQuery {
                QueryDsl.fromTemplate(sql).select {
                    val data1 = it.int("LOLperson_id")
                    val data2 = it.int("ks")
                    println("data1: $data1, data2: $data2")
                }
            }
        }
    }

    @Test
    fun test_part_code() {
        runBlocking {
            val matchText = "RU_493303533"

            R2DBC.runStrongTransaction {
                println(1)
                val guilds = Guilds().getDataOneWithoutTransaction({ tbl_guilds.id eq 1 })!!
                println(2)
                val KORD = KORDs().getDataOneWithoutTransaction({ tbl_kords.id eq 11 })!!
                println(3)
                val match = Matches().getDataOneWithoutTransaction({ tbl_matches.matchId eq matchText })!!
                println(4)
                val KORDLOLs = KORDLOLs().getDataWithoutTransaction({ tbl_kordlols.KORD_id eq KORD.id ; tbl_kordlols.guild_id eq guilds.id })
                println(5)
                val matchParts = ParticipantsNew().getDataWithoutTransaction({ tbl_participantsnew.match_id eq match.id })
                println(5.2)
                val myLOL: LOLs = LOLs().getDataOneWithoutTransaction({ tbl_lols.LOL_region eq match.region ; tbl_lols.id.inList(KORDLOLs.map { it.LOL_id }) })!!
                println(5.5)
                val participants = ParticipantsNew().getDataOneWithoutTransaction({ tbl_participantsnew.match_id eq match.id ; tbl_participantsnew.LOLperson_id eq myLOL.id })!!
                println(6)
                val mmrData = MMRs().getDataOneWithoutTransaction({ tbl_mmrs.champion eq participants.championName })!!
                println(7)
                val result = Calc_MMR(participants, match, mmrData)
                println(8)
                result.init()
                println(9)
                println("**Результаты подсчета ММР по матчу ${matchText}**\n\n${result.mmrValueTextLog}")
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
    fun test_mmr() {
        runBlocking {
            val mmr = MMRs(champion = "champ")

            mmr.create(MMRs::champion)
            printLog(mmr)
            val str = "asd"
            mmr.champion = "champ new"
            mmr.update()
            printLog(mmr)
        }
    }

    @Test
    fun checkMatchContains() {
        EnumMMRRank.entries.forEach {
            printLog("${it.nameRank} - ${it.ordinal} - ${((it.ordinal / 10.0) * 2.0) + 1.0}")
        }
    }
}