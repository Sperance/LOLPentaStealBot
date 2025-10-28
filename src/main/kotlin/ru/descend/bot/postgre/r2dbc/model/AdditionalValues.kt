package ru.descend.bot.postgre.r2dbc.model

import org.komapper.annotation.KomapperAutoIncrement
import org.komapper.annotation.KomapperEntity
import org.komapper.annotation.KomapperId
import org.komapper.annotation.KomapperTable
import org.komapper.core.dsl.Meta

@KomapperEntity
@KomapperTable("tbl_additional_values")
data class AdditionalValues(
    @KomapperId
    @KomapperAutoIncrement
    val id: Int = 0,

    var lol_id: Int = 0,
    var lol_games: Long = 0,
    var lol_wins: Long = 0,
    var lol_kills: Long = 0,
    var lol_kills3: Long = 0,
    var lol_kills4: Long = 0,
    var lol_kills5: Long = 0,
) {

    companion object {
        val tbl_additionalvalues = Meta.additionalValues
    }

    override fun toString(): String {
        return "AdditionalValues(id=$id, lol_id=$lol_id, lol_games=$lol_games, lol_wins=$lol_wins, lol_kills=$lol_kills, lol_kills3=$lol_kills3, lol_kills4=$lol_kills4, lol_kills5=$lol_kills5)"
    }
}