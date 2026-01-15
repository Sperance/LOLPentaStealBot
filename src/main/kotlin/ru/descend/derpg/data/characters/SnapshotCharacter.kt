package ru.descend.derpg.data.characters

import ru.descend.derpg.test.BaseDTO
import ru.descend.derpg.test.ItemObject
import ru.descend.derpg.test.PostMetadata

class SnapshotCharacter(
    val _id: Long,
    var _title: String,
    var _content: String,
    var _inventory: MutableList<ItemObject>,
    val _userId: Long
) : BaseDTO() {
    override fun toString(): String {
        return "SnapshotCharacter(_id=$_id, _title='$_title', _content='$_content', _inventory=$_inventory, _userId=$_userId)"
    }
}