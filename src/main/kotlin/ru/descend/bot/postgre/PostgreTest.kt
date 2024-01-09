package ru.descend.bot.postgre

import com.google.cloud.firestore.CollectionReference
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import ru.descend.bot.firebase.F_MATCHES
import ru.descend.bot.firebase.F_USERS
import ru.descend.bot.firebase.FireMatch
import ru.descend.bot.firebase.FirePerson
import ru.descend.bot.firebase.FirebaseService.collectionGuild
import ru.descend.bot.firebase.FirebaseService.getArrayFromCollection
import org.junit.Test
import ru.descend.bot.arrayCurrentMatches
import ru.descend.bot.arrayCurrentUsers
import ru.descend.bot.firebase.FirebaseService
import ru.descend.bot.launch
import ru.descend.bot.lolapi.LeagueMainObject
import ru.descend.bot.printLog
import ru.descend.bot.toFormatDateTime

class PostgreTest {

    private val arrayPersons =
        getArrayFromCollection<FirePerson>(collectionGuild("1141730148194996295", F_USERS))
    private val arrayMatches =
        getArrayFromCollection<FireMatch>(collectionGuild("1141730148194996295", F_MATCHES), 5)

    init {
        PostgreSQL.initializePostgreSQL()
    }

    @Test
    fun catchData() = runBlocking {

//        println(fireGuildTable.first())

        val myGuild = fireGuildTable.first()!!
//        println(myGuild.persons.joinToString("\n"))

//        arrayPersons.await().forEach {
//            myGuild.addPersonFire(it)
//        }

        val allMathces = getArrayFromCollection<FireMatch>(FirebaseService.firestore.collection("GUILDS/1141730148194996295/MATCHES")).await()
        val matches = myGuild.matches

//        myGuild.persons.forEach { person ->
//            println("person: ${person.LOL_puuid} ${person.LOL_name} ${person.KORD_name}")
        allMathces.forEach {matchId ->
            if (matches.find { it.matchId == matchId.matchId } != null) return@forEach
//            LeagueMainObject.catchMatch(matchId)?.let { match ->
                myGuild.addMatchFire(matchId)
//            }
        }
            LeagueMainObject.catchMatchID("_IHlZYvRrv63CpvD3-KIZ266DU3Pmk_Ohzw_NPgLg0H1Ci3jGCZNaF7oPB_nnUp4IHZhCZ_AWEb4bA", 0, 100).forEach { matchId ->
                if (matches.find { it.matchId == matchId } != null) return@forEach
                LeagueMainObject.catchMatch(matchId)?.let { match ->
                    myGuild.addMatchFire(FireMatch(match))
                }
            }
            delay(1000)
//        }

//        val pers = myGuild.matches
//        println("Person size: ${pers.size}")
//        pers.forEach {
//            println("${it.matchId} ${it.matchMode} ${it.matchDate.toFormatDateTime()}")
//            println("\t\t ${it.participants.joinToString { par -> par.puuid + " " + par.championName + " " + myGuild.name + "\n"}}")
//        }

//        arrayMatches.await().forEach {
//            println("${it.matchId} ${it.matchDate.toFormatDateTime()} ${it.listPerc.size}")
//        }
    }
}