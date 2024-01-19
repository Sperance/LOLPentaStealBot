package ru.descend.bot.postgre

import dev.kord.core.entity.Guild
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import ru.descend.bot.launch
import ru.descend.bot.postgre.PostgreSQL.getGuild
import ru.descend.bot.printLog
import ru.descend.bot.toFormatDateTime
import save
import statements.select
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
    fun testMethod() {
        tableLOLPerson.getAll { TableLOLPerson::id lessEq 3 }.forEach {
            printLog("::: $it")
        }
    }

    @Test
    fun testSimple() {
        TableKORD_LOL.deleteForLOL(1)
    }

    @Test
    fun testAddMethod() {
        tableKORDPerson.add(TableKORDPerson(KORD_id = "1"))
        tableKORDPerson.add(TableKORDPerson(KORD_id = "2"))
        tableKORDPerson.add(TableKORDPerson(KORD_id = "3"))
    }

    @Test
    fun getMatchematick() {
        tableParticipant.selectAll().orderByDescending(TableParticipant::match).limit(10).getEntities().forEach {
            printLog("Match: ${it.match?.matchId} ${it.match?.matchDate?.toFormatDateTime()} MMR: ${it.getMMR()} ${it.LOLperson?.LOL_summonerName}")
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
    fun test_coroutines() = runBlocking {
        println("0")
        launch(Dispatchers.IO) {
            while (true) {
                printLog("1")
                delay((1).seconds)
            }
        }
        launch(Dispatchers.IO) {
            while (true) {
                printLog("3")
                delay((3).seconds)
            }
        }
        launch(Dispatchers.IO) {
            while (true) {
                printLog("5")
                delay((5).seconds)
            }
        }
        printLog("1")
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