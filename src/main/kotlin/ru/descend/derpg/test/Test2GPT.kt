package ru.descend.derpg.test

import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.javatime.timestamp
import org.jetbrains.exposed.v1.json.jsonb
import java.time.Instant

interface VersionedTable {
    val version: Column<Int>
    val createdAt: Column<Instant>
}

object UsersTable : UUIDTable("users"), VersionedTable {

    val email = text("email").uniqueIndex()

    override val createdAt = timestamp("created_at")
    override val version = integer("version")
}

object AccountsTable : UUIDTable("accounts"), VersionedTable {

    val userId = reference(
        name = "user_id",
        foreign = UsersTable
    )

    val name = text("name")
    val level = integer("level")
    val experience = long("experience")

    val stats = jsonb<String>(
        name = "stats",
        jsonConfig = Json
    )

    val bonuses = jsonb<String>(
        name = "bonuses",
        jsonConfig = Json
    )

    override val createdAt = timestamp("created_at")
    override val version = integer("version")
}

object InventoriesTable : UUIDTable("inventories"), VersionedTable {

    val accountId = reference(
        name = "account_id",
        foreign = AccountsTable
    ).uniqueIndex()

    val capacity = integer("capacity")

    override val createdAt = timestamp("created_at")
    override val version = integer("version")
}

object ItemsTable : UUIDTable("items"), VersionedTable {

    val inventoryId = reference(
        name = "inventory_id",
        foreign = InventoriesTable
    )

    val baseType = text("base_type")
    val rarity = text("rarity")
    val itemLevel = integer("item_level")

    val modifiers = jsonb<String>(
        name = "modifiers",
        jsonConfig = Json
    )

    val corrupted = bool("corrupted")

    override val createdAt = timestamp("created_at")
    override val version = integer("version")
}

object AccountEquipmentTable : Table("account_equipment"), VersionedTable {

    val accountId = reference(name = "account_id", foreign = AccountsTable).uniqueIndex()

    val weaponId = reference("weapon_id", ItemsTable).nullable()
    val helmetId = reference("helmet_id", ItemsTable).nullable()
    val chestId = reference("chest_id", ItemsTable).nullable()
    val glovesId = reference("gloves_id", ItemsTable).nullable()
    val bootsId = reference("boots_id", ItemsTable).nullable()
    val ring1Id = reference("ring1_id", ItemsTable).nullable()
    val ring2Id = reference("ring2_id", ItemsTable).nullable()
    val amuletId = reference("amulet_id", ItemsTable).nullable()
    val beltId = reference("belt_id", ItemsTable).nullable()

    override val version = integer("version")
    override val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(accountId)
}

enum class ModifierSource {
    BASE,
    ACCOUNT,
    ITEM,
    PASSIVE,
    BUFF
}

enum class ModifierType {
    FLAT,
    INCREASED,
    MORE,
    OVERRIDE
}

enum class StatKey {
    STR, DEX, INT,

    LIFE, MANA,

    PHYSICAL_DAMAGE, MAGICAL_DAMAGE,

    ATTACK_TIME, ATTACK_SPEED,

    FIRE_RESIST, COLD_RESIST, LIGHTNING_RESIST
}

data class StatModifier(
    val stat: StatKey,
    val type: ModifierType,
    val value: Double,
    val source: ModifierSource
)

class StatAccumulator {

    private val flat = mutableMapOf<StatKey, Double>()
    private val increased = mutableMapOf<StatKey, Double>()
    private val more = mutableMapOf<StatKey, Double>()
    private val override = mutableMapOf<StatKey, Double>()

    fun add(mod: StatModifier) {
        when (mod.type) {
            ModifierType.FLAT ->
                flat.merge(mod.stat, mod.value, Double::plus)

            ModifierType.INCREASED ->
                increased.merge(mod.stat, mod.value, Double::plus)

            ModifierType.MORE ->
                more.merge(mod.stat, mod.value) { a, b -> a * (1 + b / 100) }

            ModifierType.OVERRIDE ->
                override[mod.stat] = mod.value
        }
    }

    fun result(base: Map<StatKey, Double>): Map<StatKey, Double> {
        val result = mutableMapOf<StatKey, Double>()

        for (stat in StatKey.entries) {
            val baseValue = base[stat] ?: 0.0

            val overridden = override[stat]
            if (overridden != null) {
                result[stat] = overridden
                continue
            }

            val flatValue = flat[stat] ?: 0.0
            val inc = increased[stat] ?: 0.0
            val moreMul = more[stat] ?: 1.0

            val value =
                (baseValue + flatValue) *
                        (1 + inc / 100) *
                        moreMul

            result[stat] = value
        }

        return result
    }
}