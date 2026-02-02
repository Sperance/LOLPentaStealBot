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
    val title = varchar("title", 255)
    val content = text("content")

    val params = jsonb<CharacterParams>(
        name = "params",
        jsonConfig = Json {
            encodeDefaults = false
        }
    )

    val inventory = jsonb<MutableList<ItemObject>>(
        name = "inventory",
        jsonConfig = Json
    )

    val stats = jsonb<StatContainer>(
        name = "stats",
        jsonConfig = Json
    ).nullable()
}

class CharacterEntity(id: EntityID<Long>) : BaseEntity<SnapshotCharacter>(id, CharactersTable) {
    var user by UserEntity referencedOn CharactersTable.user
    var title by CharactersTable.title
    var content by CharactersTable.content
    var params by CharactersTable.params
    var inventory by CharactersTable.inventory
    var stats by CharactersTable.stats
    private val equipments by EquipmentEntity referrersOn EquipmentsTable.character

    override fun toSnapshot(): SnapshotCharacter =
        SnapshotCharacter(
            _id = id.value,
            _title = title,
            _content = content,
            _params = params,
            _inventory = inventory,
            _stats = stats,
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

    override fun toString(): String {
        return "CharacterEntity(user=$user, title='$title', content='$content', params=$params, inventory=$inventory, stats=$stats)"
    }

    companion object : LongEntityClass<CharacterEntity>(CharactersTable)
}

@Serializable
data class StatContainer(
    val stats: MutableSet<Stat>,
    val statsBool: MutableSet<StatBool>
)

enum class EnumStatKey(val code: Int) {
    LIFE(10),
    MANA(11),

    STR(100),
    DEX(101),
    INT(102),

    PHYSICAL_DAMAGE(1000),
    MAGICAL_DAMAGE(1001),
    ATTACK_SPEED(1002),

    FIRE_RESIST(2001),
    COLD_RESIST(2002),
    LIGHTNING_RESIST(2003)
}

enum class EnumStatType(val code: Int) {
    FLAT(1),
    PERCENT(2),
    MORE(3)
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

            val code = parts[0].toInt()
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


enum class EnumStatBool(val code: Int) {
    IS_ALIVE(0),
    IS_BANNED(1)
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

            val code = parts[0].toInt()
            val value = parts[1].toBoolean()

            val statKey = EnumStatBool.entries.find { it.code == code } ?: throw IllegalArgumentException("Unknown EnumStatBool code: $code")

            return StatBool(statKey, value)
        } catch (e: NumberFormatException) {
            throw IllegalArgumentException("Invalid number format in StatBool: $stringValue", e)
        }
    }
}

/****************/

@Serializable
data class CharacterParams(
    var min_health: Double = 0.0,
    var inventory_size: Int = 0
)