package ru.descend.derpg.data.characters

import ru.descend.bot.addPercent
import ru.descend.derpg.data.equipments.EquipmentEntity
import ru.descend.derpg.test.BaseDTO
import ru.descend.derpg.test.ItemObject
import ru.descend.derpg.test.PostMetadata
import kotlin.code
import kotlin.collections.plusAssign

class SnapshotCharacter(
    val _id: Long,
    var _name: String,
    var _level: Short,
    var _experience: Int,
    var _params: MutableSet<ParamsStock>,
    var _buffs: MutableSet<Stat>?,
    var _bools: MutableSet<StatBool>?,
    var _equipments: List<EquipmentEntity>?,
    val _userId: Long
) : BaseDTO() {
    override fun toString(): String {
        return "SnapshotCharacter(_id=$_id, _name='$_name', _params=$_params, _buffs=$_buffs, _bools=$_bools, _userId=$_userId)"
    }

    fun calculateParamsWithBuffs(): MutableSet<ParamsStock> {
        val resultSet = _params.map { it.copy() }.toMutableSet()

        _buffs?.let { buffs ->
            buffs.forEach { buf ->
                resultSet.find { it.param.code == buf.key.code }?.let { par ->
                    when (buf.type) {
                        EnumStatType.FLAT -> par.maxValue += buf.value
                        EnumStatType.PERCENT -> par.maxValue = par.maxValue.addPercent(buf.value)
                    }
                }
            }
        }

        return resultSet
    }
}