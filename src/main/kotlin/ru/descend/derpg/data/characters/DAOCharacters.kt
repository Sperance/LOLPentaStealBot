package ru.descend.derpg.data.characters

import org.jetbrains.exposed.v1.core.statements.UpdateStatement
import ru.descend.derpg.test.ExposedBaseDao

class DAOCharacters : ExposedBaseDao<CharactersTable, CharacterEntity>(
    CharactersTable,
    CharacterEntity
) {
    override fun applyEntityToStatement(entity: CharacterEntity, stmt: UpdateStatement) {
        stmt[table.user] = entity.user.id
        stmt[table.name] = entity.name
        stmt[table.level] = entity.level
        stmt[table.experience] = entity.experience
        stmt[table.params] = entity.params
        stmt[table.buffs] = entity.buffs
    }

    @Suppress("SENSELESS_COMPARISON")
    override fun create(body: CharacterEntity.() -> Unit): CharacterEntity {
        val entity = CharacterEntity.new {
            body()

            if (params == null) params = getStockParams()
        }
        return entity
    }
}