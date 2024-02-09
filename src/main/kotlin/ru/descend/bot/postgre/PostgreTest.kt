package ru.descend.bot.postgre

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Test
import ru.descend.bot.postgre.tables.TableGuild
import ru.descend.bot.postgre.tables.TableMatch
import ru.descend.bot.postgre.tables.TableParticipant
import ru.descend.bot.postgre.tables.tableGuild
import ru.descend.bot.postgre.tables.tableMmr
import ru.descend.bot.postgre.tables.tableParticipant
import ru.descend.bot.printLog
import ru.descend.bot.savedObj.CalculateMMR
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
        printLog(1)
        val listIds = ArrayList<String>()
        listIds.add("RU_476092238")
        listIds.add("RE_476370823")
        listIds.add("RU_476367408")
    }

    @Test
    fun checkMatchContains() {
        val list = tableMmr.selectAll().getEntities()
        list.forEach {
            printLog("STR: $it")
        }
//        tableKORDPerson.getAll { TableKORDPerson::guild eq 1 }
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
    fun testUpdateTable() {
        tableParticipant.getAll().forEach {
            if (it.isBot()) {
                it.update(TableParticipant::bot){
                    bot = true
                }
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