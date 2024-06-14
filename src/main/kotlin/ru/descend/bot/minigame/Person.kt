package ru.descend.bot.minigame

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.util.UUID

@Serializable
data class PersonValues(
    val inventorySize: StockProperty = StockProperty(innerName = "Размер инвентаря", value = 500.0),
)

@Serializable
data class PersonBlobs(
    val isAlive: BlobProperty = BlobProperty("Живой", true),
    val isAccessAttack: BlobProperty = BlobProperty("Может атаковать", true),
)

@Serializable
sealed class StockItem {
    abstract var name: String
    abstract var cost: Double
    abstract var description: String
    var isCanSell: Boolean = true
}

@Serializable
open class SimpleItem(
    override var name: String = "",
    override var cost: Double = 0.0,
    override var description: String = "",
) : StockItem()

@Serializable
open class EquipItem(
    override var name: String,
    override var cost: Double = 0.0,
    override var description: String = "",

    var isEquipped: Boolean = false,
    var power: Double = 0.0,
    var stats: ArrayList<BaseProperty> = arrayListOf()
) : StockItem()

@Serializable
data class Person(
    val name: String,
    val uuid: String = UUID.randomUUID().toString(),
    var personBlobs: PersonBlobs = PersonBlobs(),
    var effects: PersonEffects = PersonEffects(),
    var values: PersonValues = PersonValues(),
    var stats: PersonStats = PersonStats()
) {
    @Transient var listeners: PersonListeners = PersonListeners(this)
    @Transient var items: ArrayList<StockItem> = arrayListOf()

    fun initForBattle() {
        calculateForItems()
        stats.initForBattle(this)
        effects.initForBattle(this)
    }

    private fun calculateForItems() {
        items.filter { it is EquipItem && it.isEquipped }.forEach {
            val itItem = it as EquipItem
            itItem.stats.forEach { prop ->
                stats.addForItems(prop)
            }
        }
    }
}