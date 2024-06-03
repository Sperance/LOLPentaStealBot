package ru.descend.bot.postgre.r2dbc.model

import org.komapper.annotation.KomapperAutoIncrement
import org.komapper.annotation.KomapperCreatedAt
import org.komapper.annotation.KomapperEntity
import org.komapper.annotation.KomapperId
import org.komapper.annotation.KomapperTable
import org.komapper.annotation.KomapperUpdatedAt
import org.komapper.core.dsl.Meta
import java.time.LocalDateTime

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

    @KomapperCreatedAt
    var createdAt: LocalDateTime = LocalDateTime.MIN,
    @KomapperUpdatedAt
    var updatedAt: LocalDateTime = LocalDateTime.MIN
) {

    companion object {
        val tbl_matches = Meta.matches
    }

    fun getRegionValue() = matchId.substringBefore("_")

    override fun toString(): String {
        return "Matches(id=$id, matchId='$matchId', matchMode='$matchMode')"
    }
}