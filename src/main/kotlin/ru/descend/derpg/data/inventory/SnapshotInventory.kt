package ru.descend.derpg.data.inventory

import ru.descend.derpg.model.ItemStock
import ru.descend.derpg.test.BaseDTO

class SnapshotInventory(
    val _id: Long,
    var _items: MutableSet<ItemStock>?,
) : BaseDTO() {
    override fun toString(): String {
        return "SnapshotInventory(_id=$_id, _items=$_items)"
    }
}
