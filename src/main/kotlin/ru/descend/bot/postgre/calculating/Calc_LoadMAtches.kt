package ru.descend.bot.postgre.calculating

import org.komapper.core.dsl.QueryDsl
import org.komapper.core.dsl.query.get
import ru.descend.bot.atomicIntLoaded
import ru.descend.bot.lolapi.LeagueMainObject
import ru.descend.bot.lolapi.dto.matchDto.MatchDTO
import ru.descend.bot.postgre.R2DBC
import ru.descend.bot.postgre.r2dbc.model.LOLs
import ru.descend.bot.printLog
import ru.descend.bot.sqlData
import ru.descend.bot.writeLog

class Calc_LoadMAtches {

    var lastStarted = 0
    suspend fun loadMatches(lols: Collection<LOLs>, startIndex: Int = 0) {
        val checkMatches = ArrayList<String>()
        if (startIndex > lastStarted) lastStarted = startIndex
        lols.forEach {
            if (it.LOL_puuid == "") return@forEach
            if (it.loaded_1000_matches && startIndex > 0) return@forEach
            atomicIntLoaded.incrementAndGet()

            LeagueMainObject.catchMatchID(it, lastStarted, 100).forEach ff@{ matchId ->
                if (!checkMatches.contains(matchId)) checkMatches.add(matchId)
            }
        }
        if (checkMatches.isNotEmpty()) {
            val listChecked = getNewMatches(checkMatches)
            printLog("[loadArrayMatches] count: ${listChecked.size}")
            if (listChecked.isEmpty()) {
                loadMatches(lols, lastStarted + 100)
            } else {
                loadArrayMatches(listChecked)
            }
        } else {
            printLog("[loadArrayMatches] is EMPTY")
        }
    }

    private suspend fun loadArrayMatches(checkMatches: ArrayList<String>) {
        checkMatches.sortBy { it }
        checkMatches.forEach { newMatch ->
            atomicIntLoaded.incrementAndGet()
            LeagueMainObject.catchMatch(newMatch)?.let { match ->
                addMatch(match)
            }
        }
    }

    private suspend fun getNewMatches(list: ArrayList<String>): ArrayList<String> {
        val dataAra = list.joinToString(prefix = "{", postfix = "}")
        val sql = "SELECT remove_matches('$dataAra'::character varying[])"
        R2DBC.runQuery {
            QueryDsl.fromTemplate(sql).select {
                val data = it.get<Array<String>>(0)
                list.clear()
                list.addAll(data?.toSet()?:arrayListOf())
            }
        }
        return list
    }

    private suspend fun addMatch(match: MatchDTO) {
        R2DBC.runTransaction {
            val newMatch = Calc_AddMatch(sqlData, match)
            newMatch.calculate()
        }
    }

    fun clearTempData() {
        atomicIntLoaded.set(0)
    }
}