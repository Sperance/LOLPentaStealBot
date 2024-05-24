package ru.descend.bot.postgre

import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.junit.Test
import org.komapper.core.dsl.QueryDsl
import ru.descend.bot.datas.DataStatRate
import ru.descend.bot.datas.Toppartisipants
import ru.descend.bot.enums.EnumMMRRank
import ru.descend.bot.postgre.openapi.AIResponse
import ru.descend.bot.datas.create
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
import ru.descend.bot.postgre.r2dbc.model.KORDs
import ru.descend.bot.postgre.r2dbc.model.KORDs.Companion.tbl_kords
import ru.descend.bot.to1Digits
import ru.descend.bot.toFormat
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
    fun test_list() {
        asyncLoadMatches(listOf("1", "2", "3", "4", "5", "6"), false)
        asyncLoadMatches(listOf("1", "2", "3", "4", "5"), false)
        asyncLoadMatches(listOf("1", "2", "3", "4"), false)
        asyncLoadMatches(listOf("1", "2", "3"), false)
    }

    @Test
    fun test_format_double() {
        val data = (646.5 / 226.0).toFormat(2)
        printLog(data)
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
    fun test_api_2() {
        val url = "https://api.proxyapi.ru/openai/v1/chat/completions"

        val client = OkHttpClient()

        val JSON = "application/json; charset=utf-8".toMediaType()
        val body = RequestBody.create(JSON, "{\n" +
                "        \"model\": \"gpt-3.5-turbo-1106\",\n" +
                "        \"messages\": [{\"role\": \"user\", \"content\": \"Напиши фентезийное поздравление Михаила с днём рождения\"}]\n" +
                "    }")
        val request = Request.Builder()
            .addHeader("Authorization", "Bearer sk-LT7VD2dmZoQtR0VXftSq4YpXnkS8xcxW")
            .url(url)
            .post(body)
            .build()

        val response = client.newCall(request).execute()
        val resultString = response.body?.string()
        val forecast = GsonBuilder().create().fromJson(resultString, AIResponse::class.java)
        println("RESULT: ${forecast.choices.first().message.content}")
    }

    @Test
    fun test_simple_api() {
        val client = OkHttpClient()

        val requestBody = FormBody.Builder()
            .add("model", "GPT-4o")
            .add("messages", "[{\"role\": \"user\", \"content\": \"Say this is a test!\"}]")
            .add("max_tokens", "1024")
            .build()

        val request = Request.Builder()
            .url("https://api.proxyapi.ru/openai")
            .addHeader("Authorization", "Bearer sk-LT7VD2dmZoQtR0VXftSq4YpXnkS8xcxW")
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()

        printLog("req: ${request.url}")

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Запрос к серверу не был успешен:" +
                            " ${response.code} ${response.message}")
                }
                // пример получения конкретного заголовка ответа
                println("Server: ${response.header("Server")}")
                // вывод тела ответа
                println(response.body!!.string())
            }
        } catch (e: IOException) {
            println("Ошибка подключения: $e");
        }
    }

    @Test
    fun test_calc_champion_winrate() {
        runBlocking {
            val lolid = 14

            val arrayARAM = HashMap<String, DataChampionWinstreak>()
            val dateCurrent = LocalDate.now()
            val modifiedDate = dateCurrent.minusMonths(1).toDate().time

            val savedParticipantsMatches = R2DBC.getParticipants { Participants.tbl_participants.LOLperson_id eq lolid }
            val arrayMatches = R2DBC.getMatches { Matches.tbl_matches.matchDateStart greaterEq modifiedDate ; Matches.tbl_matches.id.inList(savedParticipantsMatches.map { it.match_id }) ; Matches.tbl_matches.guild_id eq 1 ; Matches.tbl_matches.surrender eq false ; Matches.tbl_matches.bots eq false }
            val lastParticipants = R2DBC.getParticipants { Participants.tbl_participants.LOLperson_id eq lolid ; Participants.tbl_participants.match_id.inList(arrayMatches.map { it.id }) }
            lastParticipants.forEach {
                if (arrayMatches.find { mch -> mch.id == it.match_id }?.matchMode == "ARAM") {
                    if (arrayARAM[it.championName] == null) {
                        arrayARAM[it.championName] = DataChampionWinstreak(it.championId, 1, if (it.win) 1 else 0, it.kda)
                    } else {
                        val curData = arrayARAM[it.championName]!!
                        curData.championGames++
                        curData.championWins += if (it.win) 1 else 0
                        curData.championKDA += it.kda
                        arrayARAM[it.championName] = curData
                    }
                }
            }

            val savedParts = arrayARAM.map { it.key to it.value }.sortedByDescending { it.second.championGames }.toMap()

            savedParts.forEach { (i, pairs) ->
                printLog("$i Games: ${pairs.championGames} Winrate: ${((pairs.championWins.toDouble() / pairs.championGames) * 100.0).to1Digits()} KDA: ${(pairs.championKDA / pairs.championGames).to1Digits()}")
            }
        }
    }

    @Test
    fun test_all_stats() {
        runBlocking {
            val statClass = Toppartisipants()
            val savedKORDLOLS = R2DBC.getKORDLOLs { tbl_kordlols.guild_id eq 1 }
            val normalMatches = R2DBC.getMatches { tbl_matches.bots eq false ; tbl_matches.surrender eq false }
            val savedparticipants = R2DBC.getParticipants { tbl_participants.match_id.inList(normalMatches.map { it.id }) ; tbl_participants.LOLperson_id.inList(savedKORDLOLS.map { it.LOL_id }) }
            savedparticipants.forEach {
                statClass.calculateField(it, "Убийств", it.kills.toDouble())
                statClass.calculateField(it, "Смертей", it.deaths.toDouble())
                statClass.calculateField(it, "Ассистов", it.assists.toDouble())
                statClass.calculateField(it, "KDA", it.kda)
                statClass.calculateField(it, "Урон в минуту", it.damagePerMinute)
                statClass.calculateField(it, "Эффектных щитов/хилов", it.effectiveHealAndShielding)
                statClass.calculateField(it, "Урона строениям", it.damageDealtToBuildings.toDouble())
                statClass.calculateField(it, "Урона поглощено", it.damageSelfMitigated.toDouble())
                statClass.calculateField(it, "Секунд контроля врагам", it.enemyChampionImmobilizations.toDouble())
                statClass.calculateField(it, "Получено золота", it.goldEarned.toDouble())
                statClass.calculateField(it, "Уничтожено ингибиторов", it.inhibitorKills.toDouble())
                statClass.calculateField(it, "Критический удар", it.largestCriticalStrike.toDouble())
                statClass.calculateField(it, "Магического урона чемпионам", it.magicDamageDealtToChampions.toDouble())
                statClass.calculateField(it, "Физического урона чемпионам", it.physicalDamageDealtToChampions.toDouble())
                statClass.calculateField(it, "Чистого урона чемпионам", it.trueDamageDealtToChampions.toDouble())
                statClass.calculateField(it, "Убито миньонов", it.minionsKills.toDouble())
                statClass.calculateField(it, "Использовано заклинаний", it.skillsCast.toDouble())
                statClass.calculateField(it, "Уклонений от заклинаний", it.skillshotsDodged.toDouble())
                statClass.calculateField(it, "Попаданий заклинаниями", it.skillshotsHit.toDouble())
                statClass.calculateField(it, "Попаданий снежками", it.snowballsHit.toDouble())
//                statClass.calculateField(it, "Соло-убийств", it.soloKills.toDouble())
//                statClass.calculateField(it, "Провёл в контроле (сек)", it.timeCCingOthers.toDouble())
                statClass.calculateField(it, "Наложено щитов союзникам", it.totalDamageShieldedOnTeammates.toDouble())
                statClass.calculateField(it, "Получено урона", it.totalDamageTaken.toDouble())
                statClass.calculateField(it, "Нанесено урона чемпионам", it.totalDmgToChampions.toDouble())
                statClass.calculateField(it, "Лечение союзников", it.totalHealsOnTeammates.toDouble())
//                statClass.calculateField(it, "Контроль врагов (сек)", it.totalTimeCCDealt.toDouble())
            }

            statClass.getResults().forEach {
                printLog("$it")
            }
        }
    }

    @Test
    fun test_get_last() {
        runBlocking {
            val lolobj = R2DBC.getLOLone(declaration = {tbl_lols.LOL_summonerLevel greaterEq 1000}, first = false)
            println("lol id: ${lolobj?.id} level: ${lolobj?.LOL_summonerLevel}")
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
            val arrayMatches = R2DBC.getMatches { Matches.tbl_matches.id.inList(savedParticipantsMatches.map { it.match_id }) ; Matches.tbl_matches.guild_id eq 1 ; Matches.tbl_matches.surrender eq false ; Matches.tbl_matches.bots eq false }
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

    suspend fun sendMessageToGIGAChatAPI(accessToken: String, message: String): String {
        return withContext(Dispatchers.IO) {
            val url = URL("https://gigachat.devices.sberbank.ru/api/v1/chat/completions")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Authorization", "Bearer $accessToken")
            connection.setRequestProperty("Content-Type", "application/json")

            val requestBody = "{\"chatId\": \"your_chat_id_here\", \"author\": \"your_author_name_here\", \"content\": \"$message\"}"
            connection.doOutput = true
            val outputStream = connection.outputStream
            outputStream.write(requestBody.toByteArray())
            outputStream.close()

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val inputStream = connection.inputStream
                val response = inputStream.bufferedReader().readText()
                inputStream.close()
                response
            } else {
                val errorStream = connection.errorStream
                val errorMessage = errorStream.bufferedReader().readText()
                errorStream.close()
                throw IOException("Failed to send message to GIGAChat API. Error message: $errorMessage")
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