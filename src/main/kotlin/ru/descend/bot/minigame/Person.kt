package ru.descend.bot.minigame

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.util.UUID

@Serializable
data class PersonValues(
    val maxAttackSpeed: StockProperty = StockProperty(innerName = "Максимальная скорость атаки", value = 500.0),
    val CONSTmaxAttackSpeed: StockProperty = StockProperty(innerName = "Лимит - Максимальная скорость атаки", value = 100.0)
)

@Serializable
data class PersonBlobs(
    val isAlive: BlobProperty = BlobProperty("Живой", true),
    val isAccessAttack: BlobProperty = BlobProperty("Может атаковать", true),

    val enableCONSTmaxAttackSpeed: BlobProperty = BlobProperty("Снят лимит на максимальную скорость атаки", false),
)

@Serializable
sealed class StockItem {
    abstract var name: String
    abstract var cost: Double
    abstract var description: String
}

@Serializable
open class SimpleItem(
    override var name: String,
    override var cost: Double = 0.0,
    override var description: String = "",

    var isCanSell: Boolean = true
) : StockItem()

open class EquipItem(
    name: String,
    cost: Double = 0.0,
    description: String = "",

    var isEquipped: Boolean = false,
    var power: Double = 0.0,
    var stats: ArrayList<BaseProperty> = arrayListOf()
) : SimpleItem(name)

@Serializable
data class Person(
    val name: String,
    val uuid: String = UUID.randomUUID().toString(),
    var personValues: PersonValues = PersonValues(),
    var personBlobs: PersonBlobs = PersonBlobs(),
    var effects: PersonEffects = PersonEffects(),
    var stats: PersonStats = PersonStats()
) {
    @Transient var listeners: PersonListeners = PersonListeners(this)
    @Transient var items: ArrayList<SimpleItem> = arrayListOf()

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