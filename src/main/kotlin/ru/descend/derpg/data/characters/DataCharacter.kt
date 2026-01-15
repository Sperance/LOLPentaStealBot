package ru.descend.derpg.data.characters

import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.LongEntityClass
import org.jetbrains.exposed.v1.json.jsonb
import ru.descend.derpg.data.equipments.EquipmentEntity
import ru.descend.derpg.data.equipments.EquipmentsTable
import ru.descend.derpg.data.users.UserEntity
import ru.descend.derpg.data.users.UsersTable
import ru.descend.derpg.test.BaseEntity
import ru.descend.derpg.test.BaseTable
import ru.descend.derpg.test.ItemObject
import ru.descend.derpg.test.PostMetadata

object CharactersTable : BaseTable("characters") {
    val user = reference("user_id", UsersTable)
    val title = varchar("title", 255)
    val content = text("content")

    val inventory = jsonb<MutableList<ItemObject>>(
        name = "inventory",
        jsonConfig = Json
    )
}

class CharacterEntity(id: EntityID<Long>) : BaseEntity<SnapshotCharacter>(id, CharactersTable) {
    var user by UserEntity referencedOn CharactersTable.user
    var title by CharactersTable.title
    var content by CharactersTable.content
    var inventory by CharactersTable.inventory
    private val equipments by EquipmentEntity referrersOn EquipmentsTable.character

    override fun toSnapshot(): SnapshotCharacter =
        SnapshotCharacter(
            _id = id.value,
            _title = title,
            _content = content,
            _inventory = inventory,
            _userId = user.id.value
        ).apply {
            _createdAt = createdAt
            _updatedAt = updatedAt
            _deletedAt = deletedAt
            _version = version
        }

    fun getEquipments(): List<EquipmentEntity> {
        return try {
            equipments.toList()
        }catch (_: Exception) {
            listOf()
        }
    }

    override fun toString(): String {
        return "CharacterEntity(user=$user, title='$title', content='$content', inventory=$inventory)"
    }

    companion object : LongEntityClass<CharacterEntity>(CharactersTable)
}