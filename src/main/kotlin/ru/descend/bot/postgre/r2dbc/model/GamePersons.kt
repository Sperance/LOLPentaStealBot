package ru.descend.bot.postgre.r2dbc.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.komapper.annotation.KomapperAutoIncrement
import org.komapper.annotation.KomapperEntity
import org.komapper.annotation.KomapperId
import org.komapper.annotation.KomapperTable
import org.komapper.core.dsl.Meta
import ru.descend.bot.datas.create
import ru.descend.bot.minigame.Person
import ru.descend.bot.minigame.PersonBlobs
import ru.descend.bot.minigame.PersonEffects
import ru.descend.bot.minigame.PersonStats
import ru.descend.bot.minigame.PersonValues

@KomapperEntity
@KomapperTable("tbl_gamepersons")
data class GamePersons(
    @KomapperId
    @KomapperAutoIncrement
    val id: Int = 0,

    var name: String = "",
    var uuid: String = "",

    var personBlobs: String = "",
    var personEffects: String = "",
    var personValues: String = "",
    var personStats: String = "",
) {

    suspend fun createPerson(person: Person) {
        name = person.name
        uuid = person.uuid
        personBlobs = Json.encodeToString(person.personBlobs)
        personEffects = Json.encodeToString(person.effects)
        personValues = Json.encodeToString(person.values)
        personStats = Json.encodeToString(person.stats)
        create(GamePersons::uuid)
    }

    fun encodeToPerson() = Person(
        name = name,
        uuid = uuid,
        personBlobs = Json.decodeFromString<PersonBlobs>(personBlobs),
        effects = Json.decodeFromString<PersonEffects>(personEffects),
        values = Json.decodeFromString<PersonValues>(personValues),
        stats = Json.decodeFromString<PersonStats>(personStats)
    )

    companion object {
        val tbl_gamepersons = Meta.gamePersons
    }
}