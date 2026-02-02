package ru.descend.derpg.data.users

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.LongEntityClass
import ru.descend.derpg.data.characters.CharacterEntity
import ru.descend.derpg.data.characters.CharactersTable
import ru.descend.derpg.test.BaseEntity
import ru.descend.derpg.test.BaseTable

object UsersTable : BaseTable("users") {
    val name = varchar("name", 255)
    val email = varchar("email", 255)
}

class UserEntity(id: EntityID<Long>) : BaseEntity<SnapshotUser>(id, UsersTable) {
    var name by UsersTable.name
    var email by UsersTable.email
    private val characters by CharacterEntity referrersOn CharactersTable.user

    override fun toSnapshot(): SnapshotUser =
        SnapshotUser(
            _id = id.value,
            _name = name,
            _email = email
        ).apply {
            _createdAt = createdAt
            _updatedAt = updatedAt
            _deletedAt = deletedAt
            _version = version
        }

    fun getCharacters(): List<CharacterEntity> {
        return try {
            characters.toList()
        }catch (_: Exception) {
            listOf()
        }
    }

    override fun toString(): String {
        return "UserEntity(name='$name', email='$email')"
    }

    companion object : LongEntityClass<UserEntity>(UsersTable)
}