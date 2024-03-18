package ru.descend.bot.postgre

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Test
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import ru.descend.bot.catchToken
import ru.descend.bot.lolapi.LeagueApi
import ru.descend.bot.lolapi.dataclasses.SavedPartSteal
import ru.descend.bot.postgre.tables.TableGuild
import ru.descend.bot.postgre.tables.TableMatch
import ru.descend.bot.postgre.tables.TableParticipant
import ru.descend.bot.postgre.tables.tableGuild
import ru.descend.bot.postgre.tables.tableParticipant
import ru.descend.bot.printLog
import ru.descend.bot.savedObj.EnumMMRRank
import ru.descend.bot.savedObj.getStrongDate
import ru.descend.bot.test.ChatMessage
import ru.descend.bot.test.Message
import ru.descend.bot.test.OpenAIAPIClient
import ru.descend.bot.test.OpenAIRequestModel
import ru.descend.bot.test.OpenAIResponseModel
import ru.descend.bot.toDate
import statements.selectAll
import update
import java.io.IOException
import java.lang.ref.WeakReference
import java.time.Duration


class PostgreTest {

//    private val arrayPersons = getArrayFromCollection<FirePerson>(collectionGuild("1141730148194996295", F_USERS))
//    private val arrayMatches = getArrayFromCollection<FireMatch>(collectionGuild("1141730148194996295", F_MATCHES), 5)

    init {
//        Postgre.initializePostgreSQL()
    }

    @Test
    fun test_mmr(){
        printLog("MMR: ${EnumMMRRank.getMMRRank(42.0)}")
        printLog("MMR: ${EnumMMRRank.getMMRRank(420.0)}")
        printLog("MMR: ${EnumMMRRank.getMMRRank(0.0)}")
        printLog("MMR: ${EnumMMRRank.getMMRRank(89.5)}")
    }

    @Test
    fun testMethod() {
        printLog(1)
        val listIds = ArrayList<String>()
        listIds.add("RU_476092238")
        listIds.add("RE_476370823")
        listIds.add("RU_476367408")
    }

    @Test
    fun testsss() {
        val leagueApi = LeagueApi(catchToken()[1], LeagueApi.RU)
        val dragonService = leagueApi.dragonService

        val versions = dragonService.getVersions().execute().body()!!
        val champions = dragonService.getChampions(versions.first(), "ru_RU").execute().body()!!

        val namesAllHero = ArrayList<String>()
        champions.data::class.java.declaredFields.forEach {
            it.isAccessible = true
            val curData = it.get(champions.data)
            val nameField = curData::class.java.getDeclaredField("name")
            nameField.isAccessible = true
            namesAllHero.add(nameField.get(curData).toString())
        }
    }

    private val client = OkHttpClient()

    @Test
    fun test_() {

        val apiService = OpenAIAPIClient.create()

        val messageList = listOf(Message("user", "say hello world"))
        val requestModel = OpenAIRequestModel("gpt-3.5-turbo",messageList, 0.7f)

        val call: Call<OpenAIResponseModel> = apiService.getCompletion(requestModel)
        val res = call.execute()

        printLog("1: ${res.message()}")
        printLog("2: ${res.code()}")

//        call.enqueue(object : Callback<OpenAIResponseModel> {
//            override fun onResponse(call: Call<OpenAIResponseModel>, response: Response<OpenAIResponseModel>) {
//                if (response.isSuccessful && response.body() != null) {
//                    val responseBody = response.body()
//                    val generatedText = responseBody?.choices?.get(0)?.message?.content
//                    printLog("MSG: $generatedText")
//                } else {
//                    // Handle API error
//                    printLog("ERR1: \"API error\"")
//                }
//            }
//            override fun onFailure(call: Call<OpenAIResponseModel>, t: Throwable) {
//                // Handle network or request failure
//                printLog("MSG: \"API onFailure: ${t.message}\"")
//            }
//        })
    }

    @Test
    fun test_chatgpt() {

        runBlocking {

            val request = Request.Builder()
                .url("https://api.openai.com/v1/models")
                .get()
                .addHeader("Authorization", "Bearer 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")
                .build()

            client.newCall(request).execute().use { response ->

                printLog("networkResponse: ${response.networkResponse}")
                if (!response.isSuccessful) throw IOException("Unexpected code $response")
                for ((name, value) in response.headers) {
                    println("$name: $value")
                }

                println(response.body!!.string())
            }
        }

    }

    @Test
    fun testtimeline() {
        val leagueApi = LeagueApi(catchToken()[1], LeagueApi.RU)
        val service = leagueApi.leagueService

        val exec = service.getMatchTimeline("RU_481300486").execute()
        exec.body()?.let {
            val mapPUUID = HashMap<Long, String>()
            it.info.participants.forEach { part ->
                mapPUUID[part.participantId] = part.puuid
            }

            var lastDate = System.currentTimeMillis()
            var removedPart: SavedPartSteal? = null
            var isNextCheck = false
            val arrayQuadras = ArrayList<SavedPartSteal>()

            it.info.frames.forEach { frame ->
                frame.events.forEach lets@ { event ->
                    if (event.killerId != null && event.type.contains("CHAMPION")) {
                        val betw = Duration.between(lastDate.toDate().toInstant(), event.timestamp.toDate().toInstant())
                        val resDate = getStrongDate(event.timestamp)
                        lastDate = event.timestamp

                        printLog("EVENT: team:${if (event.killerId <= 5) "BLUE" else "RED"} killerId:${event.killerId} multiKillLength:${event.multiKillLength ?: 0} killType: ${event.killType?:""} type:${event.type} ${resDate.timeSec} STAMP: ${event.timestamp} BETsec: ${betw.toSeconds()}")

                        if (isNextCheck && (event.type == "CHAMPION_KILL" || event.type == "CHAMPION_SPECIAL_KILL")) {
                            arrayQuadras.forEach saved@ { sPart ->
                                if (sPart.team == (if (event.killerId <= 5) "BLUE" else "RED") &&
                                    sPart.participantId != event.killerId) {
                                    printLog("PENTESTEAL. Чел PUUID ${mapPUUID[event.killerId]} состилил Пенту у ${sPart.puuid}")
                                    removedPart = sPart
                                    return@saved
                                }
                            }
                            if (removedPart != null) {
                                arrayQuadras.remove(removedPart)
                                removedPart = null
                            }
                            isNextCheck = false
                        }
                        if (event.multiKillLength == 4L) {
                            arrayQuadras.add(SavedPartSteal(event.killerId, mapPUUID[event.killerId]?:"", if (event.killerId <= 5) "BLUE" else "RED", event.timestamp))
                            isNextCheck = true
                        }
                    }
                }
            }
        }
    }

    private val _data = ArrayList<TableParticipant>()
    val data = WeakReference(_data)

    @Test
    fun testMethodWeak() {

        System.gc()

        runBlocking {
            while (true) {

                data.get()?.addAll(tableParticipant.selectAll().getEntities())
                printLog("DATA REF SIZE: ${data.get()?.size} DATA SIZE: ${_data.size}")
                _data.clear()

//                dataRef.get()?.add(TableParticipant())
//                data.add(TableParticipant())

                delay(2000)
            }
        }
    }

    @Test
    fun checkMatchContains() {
        EnumMMRRank.entries.forEach {
            printLog("${it.nameRank} - ${it.ordinal} - ${((it.ordinal / 10.0) * 2.0) + 1.0}")
        }
    }

    private fun Double.fromDoubleValue(stock: Double): Double {
        return ((this / stock) * 100.0)
    }

    @Test
    fun getMatchematick() {
        tableParticipant.selectAll().where { TableParticipant::participant_uid eq "" }.getEntities().forEach {
            it.update(TableParticipant::participant_uid) {
                this.participant_uid = match?.matchId + "#" + LOLperson?.LOL_puuid + "#" + LOLperson?.id
            }
        }
    }

    @Test
    fun testSelecting() {
        tableParticipant.getAll().forEach {
            if (it.LOLperson?.LOL_puuid == "BOT" || it.LOLperson?.LOL_summonerId == "BOT" && it.match?.bots == false){
                it.match?.update(TableMatch::bots){
                    bots = true
                }
            }
        }
    }

    @Test
    fun resetTableData() {
        tableParticipant.getAll().forEach {
            it.update(TableParticipant::guildUid){
                guildUid = "1141730148194996295"
            }
        }
    }

    @Test
    fun reloadFireMatches() = runBlocking {

        val guildText = "1141730148194996295"
        val myGuild = tableGuild.first { TableGuild::idGuild eq guildText }

        if (myGuild == null) {
            printLog("Guild $guildText is not exists in SQL")
            return@runBlocking
        }

//        var tick = 0
//        getArrayFromCollection<FireMatch>(collectionGuild(guildText, F_MATCHES)).await().forEach {fm ->
//            myGuild.addMatchFire(fm)
//        }

//        LeagueMainObject.catchMatchID("dbAIlBH6Ym3yEQfn7cBBRYBJ8MCtp3Qc0tnRVfplqBrpv2Y-MmBI1pxYIH8j7rwA-au2IS_PuOzA_g", 0,10).forEach ff@ { matchId ->
//            LeagueMainObject.catchMatch(matchId)?.let { match ->
//                myGuild.addMatchFire(FireMatch(match))
//            }
//        }
    }

    @Test
    fun reloadCordLolUsers() = runBlocking {
//        val guildText = "1141730148194996295"
//        val myGuild = tableGuild.first { TableGuild::idGuild eq guildText }
//
//        if (myGuild == null) {
//            printLog("Guild $guildText is not exists in SQL")
//            return@runBlocking
//        }
//
//        getArrayFromCollection<FirePerson>(collectionGuild(guildText, F_USERS)).await().forEach {fp ->
//            val KORD = TableKORDPerson(
//                guild = tableGuild.first { TableGuild::idGuild eq guildText },
//                KORD_id = fp.KORD_id,
//                KORD_name = fp.KORD_name,
//                KORD_discriminator = fp.KORD_discriminator
//            )
//            KORD.save()
//
//            val LOL = TableLOLPerson(fp)
//            LOL.save()
//            TableKORD_LOL(KORDperson = KORD, LOLperson = LOL, guild = myGuild).save()
//        }
    }
}