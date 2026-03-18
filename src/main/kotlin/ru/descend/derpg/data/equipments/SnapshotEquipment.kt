package ru.descend.derpg.data.equipments

import ru.descend.derpg.enums.EnumEquipmentType
import ru.descend.derpg.enums.EnumItem
import ru.descend.derpg.model.ItemStock
import ru.descend.derpg.model.ParamsStock
import ru.descend.derpg.model.Stat
import ru.descend.derpg.model.StatBool
import ru.descend.derpg.test.BaseDTO
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class SnapshotEquipment(
    val _id: Long = 0,
    var _name: String,
    var _content: String,
    val _uuid: Uuid = Uuid.NIL,
    val _enumEquipmentType: EnumEquipmentType,

    var _requirements: MutableSet<Stat>? = null,
    var _params: MutableSet<ParamsStock>? = null,
    var _buffs: MutableSet<Stat>? = null,
    var _bools: MutableSet<StatBool>? = null
) : BaseDTO() {

    fun toItemStock() = ItemStock(EnumItem.EQUIP, 1)

    override fun toString(): String {
        return "SnapshotEquipment(_id=$_id, _name='$_name', _content='$_content', _uuid=$_uuid, _enumEquipmentType=$_enumEquipmentType, _requirements=$_requirements, _params=$_params, _buffs=$_buffs, _bools=$_bools)"
    }
}