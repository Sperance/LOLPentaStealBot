package ru.descend.bot.minigame

data class PersonValues(
    val maxAttackSpeed: BaseProperty = BaseProperty(innerName = "Максимальная скорость атаки", value = 500.0),
    val CONSTmaxAttackSpeed: BaseProperty = BaseProperty(innerName = "Лимит - Максимальная скорость атаки", value = 100.0)
)

data class PersonBlobs(
    val isAlive: BlobProperty = BlobProperty("Живой", true),
    val isAccessAttack: BlobProperty = BlobProperty("Может атаковать", true),

    val enableCONSTmaxAttackSpeed: BlobProperty = BlobProperty("Снят лимит на максимальную скорость атаки", false),
)

open class StockItem(var name: String, var cost: Double = 0.0, var description: String = "")

open class SimpleItem(
    name: String,
    var isCanSell: Boolean = true
) : StockItem(name)

open class EquipItem(
    name: String,
    var isEquipped: Boolean = false,
    var power: Double = 0.0,
    var stats: ArrayList<BaseProperty> = arrayListOf()
) : SimpleItem(name)

data class Person(
    val name: String,
    var personValues: PersonValues = PersonValues(),
    var personBlobs: PersonBlobs = PersonBlobs(),
) {
    var stats: PersonStats = PersonStats(this)
    var listeners: PersonListeners = PersonListeners(this)
    var effects: PersonEffects = PersonEffects(this)
    var items: ArrayList<SimpleItem> = arrayListOf()

    fun initForBattle() {
        calculateForItems()
        stats.initForBattle()
        effects.initForBattle()
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