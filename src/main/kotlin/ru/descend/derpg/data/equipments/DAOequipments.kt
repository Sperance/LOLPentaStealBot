package ru.descend.derpg.data.equipments

import org.jetbrains.exposed.v1.core.statements.UpdateStatement
import ru.descend.derpg.test.ExposedBaseDao

class DAOequipments : ExposedBaseDao<EquipmentsTable, EquipmentEntity>(
    EquipmentsTable,
    EquipmentEntity
) {
    override fun applyEntityToStatement(entity: EquipmentEntity, stmt: UpdateStatement) {
        stmt[table.character] = entity.character.id
        stmt[table.name] = entity.name
        stmt[table.content] = entity.content
        stmt[table.metadata] = entity.metadata
    }
}