package ru.descend.derpg.data.characters

import ru.descend.derpg.test.BaseDTO
import ru.descend.derpg.test.ItemObject
import ru.descend.derpg.test.PostMetadata

class SnapshotCharacter(
    val _id: Long,
    var _title: String,
    var _content: String,
    var _params: CharacterParams,
    var _inventory: MutableList<ItemObject>,
    var _stats: StatContainer?,
    val _userId: Long
) : BaseDTO() {
    override fun toString(): String {
        return "SnapshotCharacter(_id=$_id, _title='$_title', _content='$_content', _params=$_params, _inventory=$_inventory, _stats=$_stats, _userId=$_userId)"
    }
}