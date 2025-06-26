package ru.descend.bot.postgre.r2dbc.model

import org.komapper.annotation.KomapperAutoIncrement
import org.komapper.annotation.KomapperEntity
import org.komapper.annotation.KomapperId
import org.komapper.annotation.KomapperTable
import org.komapper.core.dsl.Meta
import ru.descend.bot.toFormatDate

@KomapperEntity
@KomapperTable("tbl_matches")
data class Matches(
    @KomapperId
    @KomapperAutoIncrement
    val id: Int = 0,

    var matchId: String = "",
    var matchDateStart: Long = 0,
    var matchDateEnd: Long = 0,
    var matchDuration: Int = 0,
    var matchMode: String = "",
    var matchGameVersion: String = "",
    var region: String = "",
    var bots: Boolean = false,
    var surrender: Boolean = false,
    var endOfGameResult: String = "",
    var aborted: Boolean = false,
    var mapId: Int = 0,
    var gameType: String = ""
) {

    fun isNeedCalcStats() : Boolean {
        if (bots) return false
        if (surrender) return false
        if (aborted) return false
        if (matchDuration < (60 * 8)) return false
        return true
    }

    fun isNeedCalcMMR() : Boolean {
        if (matchMode != "ARAM") return false
        return isNeedCalcStats()
    }

    companion object {
        val tbl_matches = Meta.matches
    }

    fun getRegionValue() = matchId.substringBefore("_")

    override fun toString(): String {
        return "Matches(id=$id, matchId='$matchId', matchMode='$matchMode', date='${matchDateEnd.toFormatDate()}')"
    }
}