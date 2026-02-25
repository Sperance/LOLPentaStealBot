package ru.descend.derpg.data.equipments

import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.LongEntityClass
import org.jetbrains.exposed.v1.json.jsonb
import ru.descend.derpg.data.characters.CharacterEntity
import ru.descend.derpg.data.characters.CharactersTable
import ru.descend.derpg.data.characters.ParamsStock
import ru.descend.derpg.data.characters.Stat
import ru.descend.derpg.data.characters.StatBool
import ru.descend.derpg.test.BaseEntity
import ru.descend.derpg.test.BaseTable
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
object EquipmentsTable : BaseTable("equipments") {
    val character = reference("character", CharactersTable)
    val name = varchar("name", 255)
    val content = text("content")
    val uuid = uuid("uuid").uniqueIndex().clientDefault { Uuid.random() }

    val requirements = jsonb<MutableSet<Stat>>(
        name = "requirements",
        jsonConfig = Json
    ).nullable()

    val params = jsonb<MutableSet<ParamsStock>>(
        name = "params",
        jsonConfig = Json
    ).nullable()

    val buffs = jsonb<MutableSet<Stat>>(
        name = "buffs",
        jsonConfig = Json
    ).nullable()

    val bools = jsonb<MutableSet<StatBool>>(
        name = "bools",
        jsonConfig = Json
    ).nullable()
}

@OptIn(ExperimentalUuidApi::class)
class EquipmentEntity(id: EntityID<Long>) : BaseEntity<SnapshotEquipment>(id, EquipmentsTable) {
    var character by CharacterEntity referencedOn EquipmentsTable.character
    var name by EquipmentsTable.name
    var content by EquipmentsTable.content

    var uuid by EquipmentsTable.uuid

    var requirements by EquipmentsTable.requirements
    var params by EquipmentsTable.params
    var buffs by EquipmentsTable.buffs
    var bools by EquipmentsTable.bools

    override fun toSnapshot(): SnapshotEquipment =
        SnapshotEquipment(
            _id = id.value,
            _name = name,
            _content = content,
            _uuid = uuid,
            _requirements = requirements,
            _params = params,
            _buffs = buffs,
            _bools = bools,
            _character = character.toSnapshot()
        )

    override fun toString(): String {
        return "EquipmentEntity(character=${character.id}, name='$name', content='$content', uuid=$uuid, requirements=$requirements, params=$params, buffs=$buffs, bools=$bools)"
    }

    companion object : LongEntityClass<EquipmentEntity>(EquipmentsTable)
}