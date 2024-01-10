package ru.descend.bot

import dev.kord.core.entity.Guild
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Test
import ru.descend.bot.firebase.F_MATCHES
import ru.descend.bot.firebase.F_USERS
import ru.descend.bot.firebase.FireMatch
import ru.descend.bot.firebase.FirePerson
import ru.descend.bot.firebase.FirebaseService
import ru.descend.bot.lolapi.LeagueMainObject
import ru.descend.bot.postgre.LoadPostgreHistory

class FastMethods {
    /**
     * Прогрузка 300х последних игр по всем пользователям, зарегистрированных в боте
     */
    @Test
    fun reloadMatches() = runBlocking {

        printLog("[reloadMatches] started")

        val guildId = "1141730148194996295"
        val delayTime = 2 * 61 * 1000L

        val matches = FirebaseService.getArrayFromCollection<FireMatch>(FirebaseService.collectionGuild(guildId, F_MATCHES)).await()
        val users = FirebaseService.getArrayFromCollection<FirePerson>(FirebaseService.collectionGuild(guildId, F_USERS)).await()

        users.sortBy { it.personIndex }
        printLog("[reloadMatches] загрузка займёт примерно ${((users.size * delayTime) / 1000) * users.size * 3 } сек.")

        users.forEach {person ->
            var counter = 0
            if (person.LOL_puuid.isEmpty()) return@forEach
            delay(delayTime)
            printLog("Adding first 100 matches for: ${person.LOL_name} ${person.KORD_name}")
            LeagueMainObject.catchMatchID(person.LOL_puuid, 0,100).forEach mch@ { matchId ->
                if (matches.find { it.matchId == matchId } != null) return@mch
                LeagueMainObject.catchMatch(matchId)?.let { match ->
                    FirebaseService.addMatchToGuild(guildId, match)
                    matches.add(FireMatch(match))
                    counter++
                }
            }
            printLog("added $counter matches")
            counter = 0
            delay(delayTime)
            printLog("adding second 100 matches for: ${person.LOL_name} ${person.KORD_name}")
            LeagueMainObject.catchMatchID(person.LOL_puuid, 100,100).forEach mch@ { matchId ->
                if (matches.find { it.matchId == matchId } != null) return@mch
                LeagueMainObject.catchMatch(matchId)?.let { match ->
                    FirebaseService.addMatchToGuild(guildId, match)
                    matches.add(FireMatch(match))
                    counter++
                }
            }
            printLog("added $counter matches")
            counter = 0
            delay(delayTime)
            printLog("adding third 100 matches for: ${person.LOL_name} ${person.KORD_name}")
            LeagueMainObject.catchMatchID(person.LOL_puuid, 200,100).forEach mch@ { matchId ->
                if (matches.find { it.matchId == matchId } != null) return@mch
                LeagueMainObject.catchMatch(matchId)?.let { match ->
                    FirebaseService.addMatchToGuild(guildId, match)
                    matches.add(FireMatch(match))
                    counter++
                }
            }
            printLog("added $counter matches")
            counter = 0
        }
        printLog("[reloadMatches] ended")
    }
}