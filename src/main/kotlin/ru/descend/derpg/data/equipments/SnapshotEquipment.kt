package ru.descend.derpg.data.equipments

import ru.descend.derpg.data.characters.SnapshotCharacter
import ru.descend.derpg.test.BaseDTO
import ru.descend.derpg.test.ItemObject

class SnapshotEquipment(
    val _id: Long,
    var _name: String,
    var _content: String,
    var _metadata: MutableList<ItemObject>,
    val _character: SnapshotCharacter
) : BaseDTO() {
    override fun toString(): String {
        return "SnapshotEquipment(_id=$_id, _name='$_name', _content='$_content', _metadata=$_metadata, _character=$_character)"
    }
}