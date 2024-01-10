package ru.descend.bot.postgre

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import ru.descend.bot.firebase.FireMatch
import org.junit.Test
import ru.descend.bot.firebase.F_MATCHES
import ru.descend.bot.firebase.F_USERS
import ru.descend.bot.firebase.FirePerson
import ru.descend.bot.firebase.FirebaseService.collectionGuild
import ru.descend.bot.firebase.FirebaseService.getArrayFromCollection
import ru.descend.bot.lolapi.LeagueMainObject
import ru.descend.bot.printLog
import save

class PostgreTest {

//    private val arrayPersons = getArrayFromCollection<FirePerson>(collectionGuild("1141730148194996295", F_USERS))
//    private val arrayMatches = getArrayFromCollection<FireMatch>(collectionGuild("1141730148194996295", F_MATCHES), 5)

    init {
        PostgreSQL.initializePostgreSQL()
    }

    @Test
    fun testMethod() {
        fireLOLPersonTable.getAll { FireLOLPersonTable::id lessEq 3 }.forEach {
            printLog("::: $it")
        }
    }

    @Test
    fun testSimple() {
        FireKORD_LOLPersonTable.deleteForLOL(1)
    }

    @Test
    fun testAddMethod() {

        fireKORDPersonTable.add(FireKORDPersonTable(KORD_id = "1"))
        fireKORDPersonTable.add(FireKORDPersonTable(KORD_id = "2"))
        fireKORDPersonTable.add(FireKORDPersonTable(KORD_id = "3"))

        fireKORD_LOLPersonTable.add(FireKORD_LOLPersonTable(KORDperson = FireKORDPersonTable.getForId(1), LOLperson = FireLOLPersonTable.getForId(1)))
        fireKORD_LOLPersonTable.add(FireKORD_LOLPersonTable(KORDperson = FireKORDPersonTable.getForId(2), LOLperson = FireLOLPersonTable.getForId(2)))
        fireKORD_LOLPersonTable.add(FireKORD_LOLPersonTable(KORDperson = FireKORDPersonTable.getForId(2), LOLperson = FireLOLPersonTable.getForId(3)))
        fireKORD_LOLPersonTable.add(FireKORD_LOLPersonTable(KORDperson = FireKORDPersonTable.getForId(1), LOLperson = FireLOLPersonTable.getForId(4)))
    }

    @Test
    fun reloadFireMatches() = runBlocking {

        val guildText = "1141730148194996295"
        val myGuild = fireGuildTable.first { FireGuildTable::idGuild eq guildText }

        if (myGuild == null) {
            printLog("Guild $guildText is not exists in SQL")
            return@runBlocking
        }

//        var tick = 0
        getArrayFromCollection<FireMatch>(collectionGuild(guildText, F_MATCHES)).await().forEach {fm ->
            myGuild.addMatchFire(fm)
        }

//        LeagueMainObject.catchMatchID("dbAIlBH6Ym3yEQfn7cBBRYBJ8MCtp3Qc0tnRVfplqBrpv2Y-MmBI1pxYIH8j7rwA-au2IS_PuOzA_g", 0,10).forEach ff@ { matchId ->
//            LeagueMainObject.catchMatch(matchId)?.let { match ->
//                myGuild.addMatchFire(FireMatch(match))
//            }
//        }
    }

    @Test
    fun reloadCordLolUsers() = runBlocking {

        val guildText = "1141730148194996295"
        val myGuild = fireGuildTable.first { FireGuildTable::idGuild eq guildText }

        if (myGuild == null) {
            printLog("Guild $guildText is not exists in SQL")
            return@runBlocking
        }

        getArrayFromCollection<FirePerson>(collectionGuild(guildText, F_USERS)).await().forEach {fp ->
            val KORD = FireKORDPersonTable(
                KORD_id = fp.KORD_id,
                KORD_name = fp.KORD_name,
                KORD_discriminator = fp.KORD_discriminator
            )
            KORD.save()

            val LOL = FireLOLPersonTable(fp)
            LOL.save()

            FireKORD_LOLPersonTable(
                KORDperson = fireKORDPersonTable.first { FireKORDPersonTable::KORD_id eq fp.KORD_id },
                LOLperson = fireLOLPersonTable.first { FireLOLPersonTable::LOL_puuid eq fp.LOL_puuid })
                .save()
        }

//        LeagueMainObject.catchMatchID("dbAIlBH6Ym3yEQfn7cBBRYBJ8MCtp3Qc0tnRVfplqBrpv2Y-MmBI1pxYIH8j7rwA-au2IS_PuOzA_g", 0,10).forEach ff@ { matchId ->
//            LeagueMainObject.catchMatch(matchId)?.let { match ->
//                myGuild.addMatchFire(FireMatch(match))
//            }
//        }
    }
}