package ru.descend.derpg.data.equipments

import ru.descend.derpg.data.characters.SnapshotCharacter
import ru.descend.derpg.test.BaseDTO
import ru.descend.derpg.test.ItemObject
import java.util.UUID

class SnapshotEquipment(
    val _id: Long,
    var _name: String,
    var _content: String,
    val _uuid: UUID,
    var _metadata: MutableList<ItemObject>,
    val _character: SnapshotCharacter
) : BaseDTO() {
    override fun toString(): String {
        return "SnapshotEquipment(_id=$_id, _name='$_name', _content='$_content', _uuid=$_uuid, _metadata=$_metadata, _character=$_character)"
    }
}