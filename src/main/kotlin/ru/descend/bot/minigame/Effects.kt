package ru.descend.bot.minigame

abstract class StockEffect(
    var name: String,
    val category: EnumPersonLifects
) {
    abstract fun applyEffect(current: Person, enemy: Person?, battleObj: BattleObject?)
    abstract fun removeEffect(current: Person, enemy: Person?, battleObj: BattleObject?)
}

open class BaseApplyEffect(var name: String, val listEffects: Collection<StockEffect>) {
    fun invokeCategory(current: Person, enemy: Person?, battleObj: BattleObject?, category: EnumPersonLifects) {
        listEffects.filter { it.category == category }.forEach{ it.applyEffect(current, enemy, battleObj) }
    }
}

/********************************/

class EffectAdditionalDamage(var value: Double, category: EnumPersonLifects) : StockEffect("Бонусный урон", category) {
    override fun applyEffect(current: Person, enemy: Person?, battleObj: BattleObject?) {
        current.stats.attack.addEffectValue(AdditionalValue(hashCode().toString(), value))
    }

    override fun removeEffect(current: Person, enemy: Person?, battleObj: BattleObject?) {
        current.stats.attack.removeEffectValue(hashCode().toString())
    }
}
class EffectAttackSpeedUP(var value: Double, category: EnumPersonLifects) : StockEffect("Увеличение скорости атаки", category) {
    override fun applyEffect(current: Person, enemy: Person?, battleObj: BattleObject?) {
        current.stats.attackSpeed.setEffectValue(AdditionalValue(hashCode().toString(), -value))
    }

    override fun removeEffect(current: Person, enemy: Person?, battleObj: BattleObject?) {
        current.stats.attackSpeed.removeEffectValue(hashCode().toString())
    }
}
class EffectHeal(var value: Double, category: EnumPersonLifects) : StockEffect("Восстановление здоровья", category) {
    override fun applyEffect(current: Person, enemy: Person?, battleObj: BattleObject?) {
        current.stats.health.setEffectValue(AdditionalValue(hashCode().toString(), value))
    }

    override fun removeEffect(current: Person, enemy: Person?, battleObj: BattleObject?) {}
}
class EffectDoubleDamageEveryAttack(var everyNAttack: Int, category: EnumPersonLifects = EnumPersonLifects.ON_BEFORE_DAMAGE) : StockEffect("Двойной удар", category) {
    private var counterAttack = 0
    private var isApplyed = false
    override fun applyEffect(current: Person, enemy: Person?, battleObj: BattleObject?) {
        counterAttack++
        if (counterAttack != 0 && counterAttack % everyNAttack == 0) {
            current.stats.attack.addEffectValue(AdditionalValue(hashCode().toString(), current.stats.attack.get()))
            isApplyed = true
        } else if (isApplyed) {
            removeEffect(current, enemy, battleObj)
            isApplyed = false
        }
    }

    override fun removeEffect(current: Person, enemy: Person?, battleObj: BattleObject?) {
        current.stats.attack.removeEffectValue(hashCode().toString())
    }
}

/********************************/

data class PersonEffects (
    val person: Person
) {
    private val arrayEffects = ArrayList<BaseApplyEffect>()

    fun addEffect(effect: BaseApplyEffect) {
        arrayEffects.add(effect)
    }

    fun initForBattle() {
        person.listeners.onStartBattle.addListener { person1, person2, battleObj ->
            arrayEffects.forEach { it.invokeCategory(person1, person2, battleObj, EnumPersonLifects.ON_START_BATTLE) }
        }
        person.listeners.onBeforeDamage.addListener { person1, person2, battleObj ->
            arrayEffects.forEach { it.invokeCategory(person1, person2, battleObj, EnumPersonLifects.ON_BEFORE_DAMAGE) }
        }
        person.listeners.onDealDamage.addListener { person1, person2, battleObj ->
            arrayEffects.forEach { it.invokeCategory(person1, person2, battleObj, EnumPersonLifects.ON_DEAL_DAMAGE) }
        }
        person.listeners.onTakeDamage.addListener { person1, person2, battleObj ->
            arrayEffects.forEach { it.invokeCategory(person1, person2, battleObj, EnumPersonLifects.ON_TAKE_DAMAGE) }
        }
        person.listeners.onDie.addListener { person1, person2, battleObj ->
            arrayEffects.forEach { it.invokeCategory(person1, person2, battleObj, EnumPersonLifects.ON_DIE) }
        }
    }
}