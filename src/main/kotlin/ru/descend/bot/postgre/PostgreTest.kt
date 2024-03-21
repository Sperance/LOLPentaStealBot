package ru.descend.bot.postgre

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Test
import retrofit2.Call
import ru.descend.bot.catchToken
import ru.descend.bot.lolapi.LeagueApi
import ru.descend.bot.lolapi.dataclasses.SavedPartSteal
import ru.descend.bot.printLog
import ru.descend.bot.savedObj.EnumMMRRank
import ru.descend.bot.savedObj.getStrongDate
import ru.descend.bot.toDate
import statements.selectAll
import update
import java.io.IOException
import java.lang.ref.WeakReference
import java.time.Duration


class PostgreTest {

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