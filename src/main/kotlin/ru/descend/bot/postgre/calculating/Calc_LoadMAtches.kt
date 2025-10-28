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
    suspend fun loadMatches(lols: Collection<LOLs>) {
        val checkMatches = ArrayList<String>()
        lols.forEach {
            if (it.LOL_puuid == "") return@forEach
            atomicIntLoaded.incrementAndGet()

            LeagueMainObject.catchMatchID(it, 0, 100).forEach ff@{ matchId ->
                if (!checkMatches.contains(matchId)) checkMatches.add(matchId)
            }
        }
        if (checkMatches.isNotEmpty())
            loadArrayMatches(checkMatches)
    }

    private suspend fun loadArrayMatches(checkMatches: ArrayList<String>) {
        val listChecked = getNewMatches(checkMatches)
        printLog("[loadArrayMatches] count: ${listChecked.size}")
        listChecked.sortBy { it }
        listChecked.forEach { newMatch ->
            atomicIntLoaded.incrementAndGet()
            LeagueMainObject.catchMatch(newMatch)?.let { match ->
                addMatch(match)
            }
        }
    }

    private suspend fun getNewMatches(list: ArrayList<String>): ArrayList<String> {
        val dataAra = list.joinToString(prefix = "{", postfix = "}")
        val sql = "SELECT remove_matches('$dataAra'::character varying[])"
        printLog("[SQL] $sql")
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