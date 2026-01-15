package ru.descend.derpg.data.equipments

import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.LongEntityClass
import org.jetbrains.exposed.v1.json.jsonb
import ru.descend.derpg.data.characters.CharacterEntity
import ru.descend.derpg.data.characters.CharactersTable
import ru.descend.derpg.data.users.UserEntity
import ru.descend.derpg.data.users.UsersTable
import ru.descend.derpg.test.BaseEntity
import ru.descend.derpg.test.BaseTable
import ru.descend.derpg.test.ItemObject

object EquipmentsTable : BaseTable("equipments") {
    val character = reference("character", CharactersTable)
    val name = varchar("name", 255)
    val content = text("content")

    val metadata = jsonb<MutableList<ItemObject>>(
        name = "metadata",
        jsonConfig = Json
    )
}

class EquipmentEntity(id: EntityID<Long>) : BaseEntity<SnapshotEquipment>(id, EquipmentsTable) {
    var character by CharacterEntity referencedOn EquipmentsTable.character
    var name by EquipmentsTable.name
    var content by EquipmentsTable.content
    var metadata by EquipmentsTable.metadata

    override fun toSnapshot(): SnapshotEquipment =
        SnapshotEquipment(
            _id = id.value,
            _name = name,
            _content = content,
            _metadata = metadata,
            _character = character.toSnapshot()
        )

    override fun toString(): String {
        return "EquipmentEntity(character=$character, name='$name', content='$content', metadata=$metadata)"
    }

    companion object : LongEntityClass<EquipmentEntity>(EquipmentsTable)
}