package ru.descend.bot.postgre

import dev.kord.core.entity.Guild
import dev.kord.core.entity.User
import ru.descend.bot.arrayCurrentMatches
import ru.descend.bot.arrayCurrentUsers
import ru.descend.bot.firebase.CompleteResult
import ru.descend.bot.firebase.FireMatch
import ru.descend.bot.firebase.FirePerson
import ru.descend.bot.firebase.FirebaseService
import ru.descend.bot.lolapi.LeagueMainObject
import ru.descend.bot.lolapi.leaguedata.match_dto.MatchDTO
import ru.descend.bot.printLog

object LoadPostgreHistory {

    fun getMatches(guild: Guild) : List<FireMatchTable> {
        val myGuild = fireGuildTable.first { FireGuildTable::idGuild eq guild.id.value.toString() }
        if (myGuild == null) {
            printLog(guild, "[PostgreSQL] Guild with id ${guild.id.value} not finded in SQL")
            return listOf()
        }
        return myGuild.matches
    }

    fun getGuild(guild: Guild) : FireGuildTable {
        val myGuild = fireGuildTable.first { FireGuildTable::idGuild eq guild.id.value.toString() }
        if (myGuild == null) {
            FireGuildTable().initGuild(guild)
            return fireGuildTable.first { FireGuildTable::idGuild eq guild.id.value.toString() }!!
        }
        return myGuild
    }

    fun showLeagueHistory(guild: Guild) {

//        val myGuild = fireGuildTable.first { FireGuildTable::idGuild eq guild.id.value.toString() }
//        if (myGuild == null) {
//            printLog(guild, "[PostgreSQL] Guild with id ${guild.id.value} not finded in SQL")
//            return
//        }
//
//        if (arrayMatchesPostgreSQL[guild.id.value.toString()] == null) arrayMatchesPostgreSQL[guild.id.value.toString()] = ArrayList()
//
//        if (myGuild.botChannelId.isNotEmpty()) {
//            if (arrayMatchesPostgreSQL[guild.id.value.toString()]!!.isEmpty()) {
//                arrayMatchesPostgreSQL[guild.id.value.toString()]!!.addAll(myGuild.matches)
//                val sizeC = arrayMatchesPostgreSQL[guild.id.value.toString()]!!.size
//                if (sizeC > 0) printLog(guild, "[PostgreSQL] Initalize matching size: $sizeC")
//            }
//
//            if (arrayUsersPostgreSQL[guild.id.value.toString()]!!.isEmpty()) {
//                arrayUsersPostgreSQL[guild.id.value.toString()]!!.addAll(myGuild.persons)
//                val sizeC = arrayUsersPostgreSQL[guild.id.value.toString()]!!.size
//                if (sizeC > 0) printLog(guild, "[PostgreSQL] Initalize users size: $sizeC")
//            }
//
//            val curPersons = arrayUsersPostgreSQL[guild.id.value.toString()]!!.clone() as ArrayList<FirePersonTable>
//            val curMatches = arrayMatchesPostgreSQL[guild.id.value.toString()]!!.clone() as ArrayList<FireMatchTable>
//
//            curPersons.forEach {
//                if (it.LOL_puuid == "") return@forEach
//                LeagueMainObject.catchMatchID(it.LOL_puuid, 0,3).forEach ff@{ matchId ->
//                    if (curMatches.find { mch -> mch.matchId == matchId } != null) return@ff
//                    LeagueMainObject.catchMatch(matchId)?.let { match ->
//                        myGuild.addMatchFire(FireMatch(match))
//                    }
//                }
//            }
//        }
    }
}