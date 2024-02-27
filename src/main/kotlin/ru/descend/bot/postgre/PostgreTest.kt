package ru.descend.bot.postgre

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Test
import ru.descend.bot.catchToken
import ru.descend.bot.lolapi.LeagueApi
import ru.descend.bot.lolapi.LeagueMainObject
import ru.descend.bot.mail.GMailSender
import ru.descend.bot.postgre.tables.TableGuild
import ru.descend.bot.postgre.tables.TableMatch
import ru.descend.bot.postgre.tables.TableParticipant
import ru.descend.bot.postgre.tables.tableGuild
import ru.descend.bot.postgre.tables.tableParticipant
import ru.descend.bot.printLog
import ru.descend.bot.savedObj.EnumMMRRank
import ru.descend.bot.to2Digits
import ru.descend.bot.toFormatDateTime
import statements.selectAll
import update
import kotlin.time.Duration.Companion.seconds


class PostgreTest {

//    private val arrayPersons = getArrayFromCollection<FirePerson>(collectionGuild("1141730148194996295", F_USERS))
//    private val arrayMatches = getArrayFromCollection<FireMatch>(collectionGuild("1141730148194996295", F_MATCHES), 5)

    init {
        Postgre.initializePostgreSQL()
    }

    @Test
    fun test_mmr(){
        printLog("MMR: ${EnumMMRRank.getMMRRank(42.0)}")
        printLog("MMR: ${EnumMMRRank.getMMRRank(420.0)}")
        printLog("MMR: ${EnumMMRRank.getMMRRank(0.0)}")
        printLog("MMR: ${EnumMMRRank.getMMRRank(89.5)}")
    }
    @Test
    fun sendEmail() {
        try {
            val guild = tableGuild.first()
            guild?.sendEmail("sample body message")
        }catch (e: Exception) {
            e.printStackTrace()
        }
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

        val exec = service.getMatchTimeline("RU_479019514").execute()
        exec.body()?.let {
            it.info.frames.forEach { frame ->
                frame.events.forEach { event ->
                    if (event.killerId != null && (event.type == "CHAMPION_KILL" || event.type == "CHAMPION_SPECIAL_KILL"))
                        printLog("EVENT: killerId:${event.killerId} killStreakLength:${event.killStreakLength} multiKillLength:${event.multiKillLength} type:${event.type} ${event.timestamp.toFormatDateTime()}")
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