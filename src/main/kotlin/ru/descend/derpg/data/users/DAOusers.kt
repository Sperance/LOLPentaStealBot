package ru.descend.derpg.data.users

import org.jetbrains.exposed.v1.core.statements.UpdateStatement
import ru.descend.derpg.test.ExposedBaseDao

class DAOusers : ExposedBaseDao<UsersTable, UserEntity>(
    UsersTable,
    UserEntity
) {
    override fun applyEntityToStatement(entity: UserEntity, stmt: UpdateStatement) {
        stmt[table.name] = entity.name
        stmt[table.email] = entity.email
    }
}