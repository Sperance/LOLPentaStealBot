package ru.descend.bot.postgre.r2dbc.model

import org.komapper.annotation.KomapperAutoIncrement
import org.komapper.annotation.KomapperCreatedAt
import org.komapper.annotation.KomapperEntity
import org.komapper.annotation.KomapperId
import org.komapper.annotation.KomapperTable
import org.komapper.annotation.KomapperUpdatedAt
import org.komapper.core.dsl.Meta
import org.komapper.core.dsl.QueryDsl
import org.komapper.core.dsl.query.bind
import ru.descend.bot.postgre.R2DBC
import ru.descend.bot.toFormatDate
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
    var surrender: Boolean = false
) {

    companion object {
        val tbl_matches = Meta.matches
    }

    fun getRegionValue() = matchId.substringBefore("_")

    suspend fun getParticipants() = R2DBC.runQuery {
        QueryDsl.fromTemplate("SELECT * FROM tbl_participants where match_id = /*matchid*/'test'").bind("matchid", id).selectAsEntity(Participants.tbl_participants)
    }

    override fun toString(): String {
        return "Matches(id=$id, matchId='$matchId', matchMode='$matchMode', date='${matchDateEnd.toFormatDate()}')"
    }
}