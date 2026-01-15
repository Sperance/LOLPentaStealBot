package ru.descend.derpg.data.characters

import org.jetbrains.exposed.v1.core.statements.UpdateStatement
import ru.descend.derpg.test.ExposedBaseDao

class DAOCharacters : ExposedBaseDao<CharactersTable, CharacterEntity>(
    CharactersTable,
    CharacterEntity
) {
    override fun applyEntityToStatement(entity: CharacterEntity, stmt: UpdateStatement) {
        stmt[table.user] = entity.user.id
        stmt[table.title] = entity.title
        stmt[table.content] = entity.content
        stmt[table.inventory] = entity.inventory
    }
}