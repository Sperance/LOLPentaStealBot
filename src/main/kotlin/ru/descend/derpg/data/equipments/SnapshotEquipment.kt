package ru.descend.derpg.data.equipments

import ru.descend.derpg.data.characters.ParamsStock
import ru.descend.derpg.data.characters.SnapshotCharacter
import ru.descend.derpg.data.characters.Stat
import ru.descend.derpg.data.characters.StatBool
import ru.descend.derpg.test.BaseDTO
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class SnapshotEquipment(
    val _id: Long,
    var _name: String,
    var _content: String,
    val _uuid: Uuid,

    var _requirements: MutableSet<Stat>?,
    var _params: MutableSet<ParamsStock>?,
    var _buffs: MutableSet<Stat>?,
    var _bools: MutableSet<StatBool>?,

    val _character: SnapshotCharacter
) : BaseDTO() {
    override fun toString(): String {
        return "SnapshotEquipment(_id=$_id, _name='$_name', _content='$_content', _uuid=$_uuid, _requirements=$_requirements, _params=$_params, _buffs=$_buffs, _bools=$_bools, _character=${_character._id})"
    }
}