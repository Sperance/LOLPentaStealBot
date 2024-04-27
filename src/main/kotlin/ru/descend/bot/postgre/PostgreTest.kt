package ru.descend.bot.postgre

import dev.shreyaspatil.ai.client.generativeai.GenerativeModel
import dev.shreyaspatil.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Test
import ru.descend.bot.datas.DataStatRate
import ru.descend.bot.enums.EnumMMRRank
import ru.descend.bot.lolapi.LeagueMainObject
import ru.descend.bot.postgre.r2dbc.R2DBC
import ru.descend.bot.postgre.r2dbc.create
import ru.descend.bot.postgre.r2dbc.model.KORDLOLs.Companion.tbl_kordlols
import ru.descend.bot.postgre.r2dbc.model.LOLs.Companion.tbl_lols
import ru.descend.bot.postgre.r2dbc.model.MMRs
import ru.descend.bot.postgre.r2dbc.model.Matches
import ru.descend.bot.postgre.r2dbc.model.Participants
import ru.descend.bot.postgre.r2dbc.update
import ru.descend.bot.printLog
import ru.descend.bot.savedObj.toDate
import ru.descend.bot.sendMessage
import ru.descend.bot.to2Digits
import ru.descend.bot.toFormat
import ru.descend.bot.toFormatDateTime
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.util.Date
import java.util.HashMap
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.toDuration

class PostgreTest {

    @Test
    fun test_mmrs(){
        printLog("MMR: ${EnumMMRRank.getMMRRank(42.0)}")
        printLog("MMR: ${EnumMMRRank.getMMRRank(420.0)}")
        printLog("MMR: ${EnumMMRRank.getMMRRank(0.0)}")
        printLog("MMR: ${EnumMMRRank.getMMRRank(89.5)}")
    }

    @Test
    fun test_format_double() {
        val data = (646.5 / 226.0).toFormat(2)
        printLog(data)
    }

    @Test
    fun testGemini() {
        runBlocking {
            val generativeModel = GenerativeModel(
                modelName = "gemini-pro",
                apiKey = "AIzaSyC8btIsWBw0rf69PrQ4y51Vc1B9hqHEmH0"
            )

            val inputContent = content {
                text("Напиши 3 интересных факта о жизни")
            }

            val response = generativeModel.generateContent(inputContent)
            print(response.text)
        }
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

    @Test
    fun test_gigachat() {
        runBlocking {
            val accessToken = "eyJjdHkiOiJqd3QiLCJlbmMiOiJBMjU2Q0JDLUhTNTEyIiwiYWxnIjoiUlNBLU9BRVAtMjU2In0.h8DOz9eULFGnbgcii2UWKajf6khy-oksX4ql_1FBtmuUDG3mwG8KsWe9G0zeki1V2H_6ysSLhObarXOMf4N92LnwZOJ7wXAiSztSSbs8WQq29fRwNQY08CJ4oKO6ozxjuMkOlsOUO_8Q8-06-h-6mdzHkKcsKF2Vp9WlUePKnux60vhqhvJy7i8zbh1xylxkiHOfoX25VrKdlcT_jHCStkBzqauwxwsizZyGCvoxCWUIbbJxtRYgqKFggEXuhO3k72MANaB8Unwv4vQIKKoXRyPh34az9rnKXJgCFtuTFz_f-ok-z7DuGaoYfehRvzS8xkTHrFJ2JW4iHRfTZx0pVQ.XEgfNt1LPKc1qWHRiMqgng.OOA6oclVQM-iDfIYlAuvTmjQz25N3V0X5sFBeWH0L9bCN8Xjj74aAiZaWO-gMwCDw3JSTOO_z3pIhBVn0t5lusfj79BwI7x8A8Tgeaa6xCA2rOS9jHVhg-zzg4RS0x6wg0UqBr_0EkgOme2Wa6kPyjxs3JgWaSA-8CPKRNqVyoI2htr0LZk-BJTkDqMQVSnmqrsE-5dcYYvVjg3e8tYzVOlt_dswkpY0vklReUHmTppDx2329cVihxovRA1t4chw2-rpMvSDGDvQPo0Sr3RCGbo4RUNuf6PUibpxhml3qWFu4fQhrjgXkZKdN9IIUHrKrh0EZ39Q_8R_wfuD3s2JkrVuqreE1I78bHDxwrglSZ_JpoMFQRx1kpmHOVMb_KoapIdufXKKSQEpbMQ5zQYh4o5l9aYksQUaTcLvZmGVwrGztsWz_6X9NI7TVEIovOGYnHz4dShH9zYrNcqQgnc28xDcnIO0Rg3aLxXYkoRLIsCJdgEb6Zds5u0aTuyk9PhOdFEmk87MPAGiqacWIagrpoJ9kg44yInj8p09npd_XWhG0jUuX47DhGDWUDjqJnkZXK2CpF9W0_8qXOcHBu2uHl-JE_cPqZg5OS4xfdaiBkMX1Vx_SGFoYTQLDBpIvvjQcAeR9jXX53Sx5LUo2FnHf8c89a1KID5PaEo8mFv87bvbUAOIZ_Jk2_KN3TqQ5bpCRhwNe-ofiRfT_hNrifc6TAnqmMqt5uXv6Mdka4fONtY.dbryMaCsb0o-2Ql4WdrIAUUwQEVWFXiC6P_Lx6H1WkY"
            val message = "Hello, GIGAChat!"

            val response = sendMessageToGIGAChatAPI(accessToken, message)
            println("Response from GIGAChat API: $response")
        }
    }

    data class DataChampionWinstreak(
        val championId: Int,
        var championGames: Int,
        var championWins: Int,
        var championKDA: Double
    )

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
                printLog("$i Games: ${pairs.championGames} Winrate: ${((pairs.championWins.toDouble() / pairs.championGames) * 100.0).to2Digits()} KDA: ${(pairs.championKDA / pairs.championGames).to2Digits()}")
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

            arrayStat.sortByDescending { (it.winGames / it.allGames * 100.0).to2Digits() }

            arrayStat.forEach {
                printLog("** ${R2DBC.getLOLs { tbl_lols.id eq it.lol_id }.firstOrNull()?.getCorrectName()}** ${(it.winGames / it.allGames * 100.0).to2Digits()}% Games:${it.allGames}")
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