package ru.descend.derpg.data.characters

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.LongEntityClass
import org.jetbrains.exposed.v1.json.jsonb
import ru.descend.bot.addPercent
import ru.descend.derpg.data.equipments.EquipmentEntity
import ru.descend.derpg.data.equipments.EquipmentsTable
import ru.descend.derpg.data.users.UserEntity
import ru.descend.derpg.data.users.UsersTable
import ru.descend.derpg.test.BaseEntity
import ru.descend.derpg.test.BaseTable
import ru.descend.derpg.test.ItemObject
import ru.descend.derpg.test.PostMetadata

object CharactersTable : BaseTable("characters") {
    val user = reference("user_id", UsersTable)
    val name = varchar("name", 32)

    val level = short("level").default(1)
    val experience = integer("experience").default(0)

    val params = jsonb<MutableSet<ParamsStock>>(
        name = "params",
        jsonConfig = Json
    )

    val buffs = jsonb<StatContainer>(
        name = "buffs",
        jsonConfig = Json
    ).nullable()
}

class CharacterEntity(id: EntityID<Long>) : BaseEntity<SnapshotCharacter>(id, CharactersTable) {
    var user by UserEntity referencedOn CharactersTable.user
    var name by CharactersTable.name
    var level by CharactersTable.level
    var experience by CharactersTable.experience
    var params by CharactersTable.params
    var buffs by CharactersTable.buffs
    private val equipments by EquipmentEntity referrersOn EquipmentsTable.character

    override fun toSnapshot(): SnapshotCharacter =
        SnapshotCharacter(
            _id = id.value,
            _name = name,
            _level = level,
            _experience = experience,
            _params = params,
            _buffs = buffs,
            _equipments = getEquipments(),
            _userId = user.id.value
        ).apply {
            _createdAt = createdAt
            _updatedAt = updatedAt
            _deletedAt = deletedAt
            _version = version
        }

    fun getEquipments(): List<EquipmentEntity> {
        return try {
            equipments.toList()
        }catch (_: Exception) {
            listOf()
        }
    }

    fun getStockParams(): MutableSet<ParamsStock> {
        val stock = mutableSetOf<ParamsStock>()
        stock.add(ParamsStock(EnumStatKey.LIFE, 0.0, 100.0))
        stock.add(ParamsStock(EnumStatKey.STR, 1.0, 1.0))
        stock.add(ParamsStock(EnumStatKey.DEX, 1.0, 1.0))
        stock.add(ParamsStock(EnumStatKey.INT, 1.0, 1.0))
        stock.add(ParamsStock(EnumStatKey.INVENTORY_SIZE, 10.0, 10.0))
        stock.add(ParamsStock(EnumStatKey.CRIT_DAMAGE, 100.0, 200.0))
        stock.add(ParamsStock(EnumStatKey.ATTACK_SPEED, 0.1, 1.0))
        return stock
    }

    override fun toString(): String {
        return "CharacterEntity(user=$user, name='$name', params=$params, buffs=$buffs)"
    }

    companion object : LongEntityClass<CharacterEntity>(CharactersTable)
}

@Serializable
data class StatContainer(
    val stats: MutableSet<Stat>,
    val statsBool: MutableSet<StatBool>
)

enum class EnumStatKey(val code: String, val description: String) {
    LIFE("A1", "Здоровье"),
    MANA("A2", "Мана"),
    RAGE("A3", "Энергия"),

    STR("B1", "Сила"),
    DEX("B2", "Ловкость"),
    INT("B3", "Интеллект"),

    CRIT_CHANCE("C0", "Шанс критического удара"),
    CRIT_DAMAGE("C1", "Критический урон"),
    PHYSICAL_DAMAGE("C2", "Физический урон"),
    MAGICAL_DAMAGE("C3", "Магический урон"),
    ATTACK_SPEED("C4", "Скорость атаки"),

    MAGIC_RESIST("D0", "Сопротивление магии"),
    FIRE_RESIST("D1", "Сопротивление огню"),
    COLD_RESIST("D2", "Сопротивление холоду"),
    LIGHTNING_RESIST("D3", "Сопротивление молнии"),
    CHARM_RESIST("D4", "Сопротивление хаосу"),

    INVENTORY_SIZE("E1", "Размер инвентаря")
}

enum class EnumStatType(val code: Int) {
    FLAT(1),
    PERCENT(2)
}

@Serializable(with = CompactStatSerializer::class)
data class Stat(
    val key: EnumStatKey,
    val type: EnumStatType,
    var value: Double
) {
    override fun toString(): String {
        return "Stat(key=$key, type=$type, value=$value)"
    }
}

object CompactStatSerializer : KSerializer<Stat> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Stat", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Stat) {
        encoder.encodeString("${value.key.code}:${value.type.code}:${value.value}")
    }

    override fun deserialize(decoder: Decoder): Stat {
        val stringValue = decoder.decodeString()

        try {
            val parts = stringValue.split(":", limit = 3)
            if (parts.size != 3) {
                throw IllegalArgumentException("Invalid Stat format: $stringValue. Expected 'code:type:value'")
            }

            val code = parts[0]
            val type = parts[1].toInt()
            val value = parts[2].toDouble()

            val statKey = EnumStatKey.entries.find { it.code == code } ?: throw IllegalArgumentException("Unknown StatKey code: $code")
            val statType = EnumStatType.entries.find { it.code == type } ?: throw IllegalArgumentException("Unknown StatType code: $type")

            return Stat(statKey, statType, value)
        } catch (e: NumberFormatException) {
            throw IllegalArgumentException("Invalid number format in Stat: $stringValue", e)
        }
    }
}

/**************/


enum class EnumStatBool(val code: String) {
    IS_ALIVE("BL0"),
    IS_BANNED("BL1")
}

@Serializable(with = CompactStatBoolSerializer::class)
data class StatBool(
    val key: EnumStatBool,
    var value: Boolean
) {
    override fun toString(): String {
        return "StatBool(key=$key, value=$value)"
    }
}

object CompactStatBoolSerializer : KSerializer<StatBool> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("StatBool", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: StatBool) {
        encoder.encodeString("${value.key.code}:${value.value}")
    }

    override fun deserialize(decoder: Decoder): StatBool {
        val stringValue = decoder.decodeString()

        try {
            val parts = stringValue.split(":", limit = 2)
            if (parts.size != 2) {
                throw IllegalArgumentException("Invalid StatBool format: $stringValue. Expected 'code:value'")
            }

            val code = parts[0]
            val value = parts[1].toBoolean()

            val statKey = EnumStatBool.entries.find { it.code == code } ?: throw IllegalArgumentException("Unknown EnumStatBool code: $code")

            return StatBool(statKey, value)
        } catch (e: NumberFormatException) {
            throw IllegalArgumentException("Invalid number format in StatBool: $stringValue", e)
        }
    }
}

/****************/

@Serializable(with = CompactParamsStockSerializer::class)
data class ParamsStock(
    var param: EnumStatKey,
    var minValue: Double,
    var maxValue: Double
) {
    fun copy(): ParamsStock {
        return ParamsStock(
            param = this.param,
            minValue = this.minValue,
            maxValue = this.maxValue
        )
    }
}

object CompactParamsStockSerializer : KSerializer<ParamsStock> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("ParamsStock", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ParamsStock) {
        encoder.encodeString("${value.param.code}:${value.minValue}:${value.maxValue}")
    }

    override fun deserialize(decoder: Decoder): ParamsStock {
        val stringValue = decoder.decodeString()

        try {
            val parts = stringValue.split(":", limit = 3)
            if (parts.size != 3) {
                throw IllegalArgumentException("Invalid ParamsStock format: $stringValue. Expected 'code:value'")
            }

            val code = parts[0]
            val minValue = parts[1].toDouble()
            val maxValue = parts[2].toDouble()

            val statKey = EnumStatKey.entries.find { it.code == code } ?: throw IllegalArgumentException("Unknown EnumParamsStock code: $code")

            return ParamsStock(statKey, minValue, maxValue)
        } catch (e: NumberFormatException) {
            throw IllegalArgumentException("Invalid number format in ParamsStock: $stringValue", e)
        }
    }
}
