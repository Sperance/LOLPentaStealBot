package ru.descend.bot.postgre.r2dbc.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.komapper.annotation.KomapperAutoIncrement
import org.komapper.annotation.KomapperCreatedAt
import org.komapper.annotation.KomapperEntity
import org.komapper.annotation.KomapperId
import org.komapper.annotation.KomapperTable
import org.komapper.annotation.KomapperUpdatedAt
import org.komapper.core.dsl.Meta
import org.komapper.core.dsl.QueryDsl
import org.komapper.core.dsl.query.bind
import ru.descend.bot.datas.create
import ru.descend.bot.minigame.Person
import ru.descend.bot.minigame.PersonBlobs
import ru.descend.bot.minigame.PersonEffects
import ru.descend.bot.minigame.PersonStats
import ru.descend.bot.minigame.PersonValues
import ru.descend.bot.postgre.R2DBC
import ru.descend.bot.toFormatDate
import java.time.LocalDateTime
import java.util.UUID

@KomapperEntity
@KomapperTable("tbl_gamepersons")
data class GamePersons(
    @KomapperId
    @KomapperAutoIncrement
    val id: Int = 0,

    var name: String = "",
    var uuid: String = "",
    var personValues: String = "",
    var personBlobs: String = "",
    var personEffects: String = "",
    var personStats: String = "",
) {

    suspend fun createPerson(person: Person) {
        name = person.name
        uuid = person.uuid
        personValues = Json.encodeToString(person.personValues)
        personBlobs = Json.encodeToString(person.personBlobs)
        personEffects = Json.encodeToString(person.effects)
        personStats = Json.encodeToString(person.stats)
        create(GamePersons::uuid)
    }

    fun encodeToPerson() = Person(
        name = name,
        uuid = uuid,
        personValues = Json.decodeFromString<PersonValues>(personValues),
        personBlobs = Json.decodeFromString<PersonBlobs>(personBlobs),
        effects = Json.decodeFromString<PersonEffects>(personEffects),
        stats = Json.decodeFromString<PersonStats>(personStats)
    )

    companion object {
        val tbl_gamepersons = Meta.gamePersons
    }
}