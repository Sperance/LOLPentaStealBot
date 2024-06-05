package ru.descend.bot.postgre

import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.junit.Test
import org.komapper.core.dsl.QueryDsl
import org.komapper.core.dsl.query.Query
import org.komapper.core.dsl.query.bind
import org.komapper.core.dsl.query.double
import org.komapper.core.dsl.query.get
import org.komapper.core.dsl.query.int
import org.komapper.core.dsl.query.string
import org.komapper.core.dsl.visitor.QueryVisitor
import ru.descend.bot.datas.DataStatRate
import ru.descend.bot.datas.Result
import ru.descend.bot.datas.Toppartisipants
import ru.descend.bot.enums.EnumMMRRank
import ru.descend.bot.postgre.openapi.AIResponse
import ru.descend.bot.datas.create
import ru.descend.bot.datas.safeApiCall
import ru.descend.bot.postgre.r2dbc.model.KORDLOLs.Companion.tbl_kordlols
import ru.descend.bot.postgre.r2dbc.model.LOLs.Companion.tbl_lols
import ru.descend.bot.postgre.r2dbc.model.MMRs
import ru.descend.bot.postgre.r2dbc.model.Matches
import ru.descend.bot.postgre.r2dbc.model.Matches.Companion.tbl_matches
import ru.descend.bot.postgre.r2dbc.model.Participants
import ru.descend.bot.postgre.r2dbc.model.Participants.Companion.tbl_participants
import ru.descend.bot.datas.update
import ru.descend.bot.printLog
import ru.descend.bot.datas.toDate
import ru.descend.bot.generateAIText
import ru.descend.bot.lolapi.LeagueMainObject
import ru.descend.bot.lolapi.dto.InterfaceChampionBase
import ru.descend.bot.postgre.r2dbc.model.Heroes
import ru.descend.bot.postgre.r2dbc.model.KORDLOLs
import ru.descend.bot.postgre.r2dbc.model.KORDs
import ru.descend.bot.postgre.r2dbc.model.KORDs.Companion.tbl_kords
import ru.descend.bot.to1Digits
import ru.descend.bot.toFormat
import ru.gildor.coroutines.okhttp.await
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.util.HashMap

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

    @Test
    fun test_quety() {
        runBlocking {
            val query = QueryDsl
                .from(tbl_participants)
                .innerJoin(tbl_matches) { tbl_matches.id eq tbl_participants.match_id }
                .innerJoin(tbl_kordlols) { tbl_kordlols.LOL_id eq tbl_participants.LOLperson_id }
                .where { tbl_matches.matchMode.inList(listOf("ARAM", "CLASSIC")) ; tbl_matches.bots eq false ; tbl_matches.surrender eq false }
                .selectAsEntity(tbl_participants)

            val result = R2DBC.runQuery { query }
            println("res size: ${result.size}")
        }
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
                    tags = curData.tags.joinToString(", ")
                )
                hero.create(Heroes::key)
            }
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
    fun test_get_last() {
        runBlocking {
            val lolobj = R2DBC.getLOLone(declaration = {tbl_lols.LOL_summonerLevel greaterEq 1000}, first = false)
            println("lol id: ${lolobj?.id} level: ${lolobj?.LOL_summonerLevel}")
        }
    }

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
    fun test_free_api() {
        runBlocking {

            val requestText = "Напиши очень длинное и оригинальное поздравление ко Дню Рождения"

//            val url = "https://api.proxyapi.ru/openai/v1/chat/completions"
//            val url = "http://localhost:3040/v1/chat/completions"
            val url = "https://api.pawan.krd/v1/chat/completions"
            val JSON = "application/json; charset=utf-8".toMediaType()
            val body = RequestBody.create(JSON, "{\n" +
                    "        \"model\": \"gpt-3.5-turbo\",\n" +
//                    "        \"model\": \"gpt-3.5-turbo-1106\",\n" +
                    "        \"messages\": [{\"role\": \"user\", \"content\": \"$requestText\"}]\n" +
                    "    }")
            val request = Request.Builder()
                .addHeader("Authorization", "Bearer sk-LT7VD2dmZoQtR0VXftSq4YpXnkS8xcxW")
                .url(url)
                .post(body)
                .build()

            val response = OkHttpClient().newCall(request).await()

            println("code: ${response.code}")
            println("message: ${response.message}")

            val resultString = response.body?.string()
            println("result: $resultString")
            val forecast = GsonBuilder().create().fromJson(resultString, AIResponse::class.java)
            println(forecast.choices.first().message.content)
        }
    }

    @Test
    fun test_last_match() {
        val match = "RU_12321334"
        val code = match.substringAfter("_").toLong()
        for (i in 1..10) {
            println("RU_${code + i}")
        }
    }

    @Test
    fun test_AI_context() {
        runBlocking {
            val message = generateAIText("напиши необычное и оригинальное и длинное поздравление игрока Атлант с Пентакиллом за чемпиона Анивия в игре League of Legends от имени Discord сервера АрамоЛолево")
            printLog(message)
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
    fun test_parts() {
        runBlocking {
            val query = QueryDsl
                .from(tbl_participants)
                .innerJoin(tbl_matches) { tbl_matches.id eq tbl_participants.match_id }
                .where { tbl_matches.matchMode.inList(listOf("ARAM", "CLASSIC")) ; tbl_matches.bots eq false ; tbl_matches.surrender eq false }
                .orderBy(tbl_participants.id)
                .selectAsEntity(tbl_participants)

            val statClass = Toppartisipants()
            val result = R2DBC.runQuery { query }
            println("Data Size: ${result.size}")
            result.forEach {
                statClass.calculateField(it, "Убийств", it.kills.toDouble())
                statClass.calculateField(it, "Смертей", it.deaths.toDouble())
                statClass.calculateField(it, "Ассистов", it.assists.toDouble())
                statClass.calculateField(it, "KDA", it.kda)
                statClass.calculateField(it, "Урон в минуту", it.damagePerMinute)
//        statClass.calculateField(it, "Эффектных щитов/хилов", it.effectiveHealAndShielding)
                statClass.calculateField(it, "Урона строениям", it.damageDealtToBuildings.toDouble())
                statClass.calculateField(it, "Урона поглощено", it.damageSelfMitigated.toDouble())
                statClass.calculateField(it, "Секунд контроля врагам", it.enemyChampionImmobilizations.toDouble())
                statClass.calculateField(it, "Получено золота", it.goldEarned.toDouble())
//        statClass.calculateField(it, "Уничтожено ингибиторов", it.inhibitorKills.toDouble())
                statClass.calculateField(it, "Критический удар", it.largestCriticalStrike.toDouble())
                statClass.calculateField(it, "Магического урона чемпионам", it.magicDamageDealtToChampions.toDouble())
                statClass.calculateField(it, "Физического урона чемпионам", it.physicalDamageDealtToChampions.toDouble())
                statClass.calculateField(it, "Чистого урона чемпионам", it.trueDamageDealtToChampions.toDouble())
                statClass.calculateField(it, "Убито миньонов", it.minionsKills.toDouble())
                statClass.calculateField(it, "Использовано заклинаний", it.skillsCast.toDouble())
                statClass.calculateField(it, "Уклонений от заклинаний", it.skillshotsDodged.toDouble())
                statClass.calculateField(it, "Попаданий заклинаниями", it.skillshotsHit.toDouble())
                statClass.calculateField(it, "Попаданий снежками", it.snowballsHit.toDouble())
//        statClass.calculateField(it, "Соло-убийств", it.soloKills.toDouble())
//        statClass.calculateField(it, "Провёл в контроле (сек)", it.timeCCingOthers.toDouble())
                statClass.calculateField(it, "Наложено щитов союзникам", it.totalDamageShieldedOnTeammates.toDouble())
                statClass.calculateField(it, "Получено урона", it.totalDamageTaken.toDouble())
                statClass.calculateField(it, "Нанесено урона чемпионам", it.totalDmgToChampions.toDouble())
                statClass.calculateField(it, "Лечение союзников", it.totalHealsOnTeammates.toDouble())
//        statClass.calculateField(it, "Контроль врагов (сек)", it.totalTimeCCDealt.toDouble())
            }

            var resultText = ""
            statClass.getResults().forEach {
                resultText += "* $it\n"
                println("* $it\n")
            }
        }
    }

    @Test
    fun test_proc() {
        runBlocking {
            R2DBC.executeProcedure("call \"GetAVGs\"()")
        }
    }

    @Test
    fun test_duplicates() {
        runBlocking {
            val objectDB = KORDs()
            objectDB.guild_id = 2
            objectDB.KORD_id = "KORDID2"
            objectDB.KORD_name = "null"
            objectDB.donations = 666.0
            R2DBC.runQuery {
                QueryDsl.insert(tbl_kords).onDuplicateKeyUpdate(tbl_kords.donations).set { excl ->
                    tbl_kords.KORD_id eq "KORDID2"
                }.single(objectDB)
            }
        }
    }

    @Test
    fun test_calc_winrate() {
        runBlocking {
            val lolid = 14

            val arrayARAM = HashMap<Int, ArrayList<Pair<Int, Int>>>()

            val allKORDLOLS = R2DBC.getKORDLOLs { tbl_kordlols.guild_id eq 1; tbl_kordlols.LOL_id notEq lolid }
            val savedParticipantsMatches = R2DBC.getParticipants { Participants.tbl_participants.LOLperson_id eq lolid }
            val arrayMatches = R2DBC.getMatches { Matches.tbl_matches.id.inList(savedParticipantsMatches.map { it.match_id }) ; Matches.tbl_matches.surrender eq false ; Matches.tbl_matches.bots eq false }
            val lastParticipants = R2DBC.getParticipants { Participants.tbl_participants.match_id.inList(arrayMatches.map { it.id }) ; Participants.tbl_participants.LOLperson_id.inList(allKORDLOLS.map { it.LOL_id }) }
            lastParticipants.forEach {
                if (arrayMatches.find { mch -> mch.id == it.match_id }?.matchMode == "ARAM") {
                    if (arrayARAM[it.LOLperson_id] == null) {
                        arrayARAM[it.LOLperson_id] = ArrayList()
                        arrayARAM[it.LOLperson_id]!!.add(Pair(if (it.win) 1 else 0, if (!it.win) 1 else 0))
                    } else {
                        arrayARAM[it.LOLperson_id]!!.add(Pair(if (it.win) 1 else 0, if (!it.win) 1 else 0))
                    }
                }
            }

            val arrayStat = ArrayList<DataStatRate>()
            arrayARAM.forEach { (i, pairs) ->
                var winGames = 0.0
                pairs.forEach {
                    if (it.first == 1) winGames++
                }
                arrayStat.add(DataStatRate(lol_id = i, allGames = pairs.size, winGames = winGames))
            }

            arrayStat.sortByDescending { (it.winGames / it.allGames * 100.0).to1Digits() }

            arrayStat.forEach {
                printLog("** ${R2DBC.getLOLs { tbl_lols.id eq it.lol_id }.firstOrNull()?.getCorrectName()}** ${(it.winGames / it.allGames * 100.0).to1Digits()}% Games:${it.allGames}")
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