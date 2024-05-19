package ru.descend.bot.postgre.r2dbc.model

import org.komapper.annotation.KomapperAutoIncrement
import org.komapper.annotation.KomapperCreatedAt
import org.komapper.annotation.KomapperEntity
import org.komapper.annotation.KomapperId
import org.komapper.annotation.KomapperTable
import org.komapper.annotation.KomapperUpdatedAt
import org.komapper.core.dsl.Meta
import ru.descend.bot.postgre.R2DBC
import ru.descend.bot.postgre.r2dbc.model.Participants.Companion.tbl_participants
import java.time.LocalDateTime

@KomapperEntity
@KomapperTable("tbl_matches")
data class Matches(
    @KomapperId
    @KomapperAutoIncrement
    val id: Int = 0,

    var guild_id: Int = -1,

    var matchId: String = "",
    var matchDateStart: Long = 0,
    var matchDateEnd: Long = 0,
    var matchDuration: Int = 0,
    var matchMode: String = "",
    var matchGameVersion: String = "",
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

    suspend fun getParticipants() = R2DBC.getParticipants { tbl_participants.guild_id eq guild_id ; tbl_participants.match_id eq id }

    override fun toString(): String {
        return "Matches(id=$id, matchId='$matchId', matchMode='$matchMode', guild_id=$guild_id)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Matches

        if (id != other.id) return false
        if (guild_id != other.guild_id) return false
        if (matchId != other.matchId) return false
        if (matchDateStart != other.matchDateStart) return false
        if (matchDateEnd != other.matchDateEnd) return false
        if (matchDuration != other.matchDuration) return false
        if (matchMode != other.matchMode) return false
        if (matchGameVersion != other.matchGameVersion) return false
        if (bots != other.bots) return false
        if (surrender != other.surrender) return false
        if (createdAt != other.createdAt) return false
        if (updatedAt != other.updatedAt) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + guild_id
        result = 31 * result + matchId.hashCode()
        result = 31 * result + matchDateStart.hashCode()
        result = 31 * result + matchDateEnd.hashCode()
        result = 31 * result + matchDuration
        result = 31 * result + matchMode.hashCode()
        result = 31 * result + matchGameVersion.hashCode()
        result = 31 * result + bots.hashCode()
        result = 31 * result + surrender.hashCode()
        result = 31 * result + createdAt.hashCode()
        result = 31 * result + updatedAt.hashCode()
        return result
    }
}