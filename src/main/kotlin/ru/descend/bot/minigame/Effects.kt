package ru.descend.bot.minigame

import kotlinx.serialization.Serializable

@Serializable
sealed class StockEffect {
    abstract var name: String
    abstract val category: EnumPersonLifects
    open fun applyEffect(current: Person, enemy: Person?, battleObj: BattleObject?) {}
    open fun removeEffect(current: Person, enemy: Person?, battleObj: BattleObject?) {}
}

@Serializable
open class BaseApplyEffect(var name: String, val listEffects: Collection<StockEffect>) {
    fun invokeCategory(current: Person, enemy: Person?, battleObj: BattleObject?, category: EnumPersonLifects) {
        listEffects.filter { it.category == category }.forEach{ it.applyEffect(current, enemy, battleObj) }
    }
}

/********************************/
@Serializable
class EffectAdditionalDamage(
    var value: Double,
    override val category: EnumPersonLifects,
    override var name: String = "Бонусный урон"
) : StockEffect() {
    override fun applyEffect(current: Person, enemy: Person?, battleObj: BattleObject?) {
        current.stats.attack.addEffectValue(AdditionalValue(hashCode().toString(), value))
    }

    override fun removeEffect(current: Person, enemy: Person?, battleObj: BattleObject?) {
        current.stats.attack.removeEffectValue(hashCode().toString())
    }
}
@Serializable
class EffectAttackSpeedUP(
    var value: Double,
    override val category: EnumPersonLifects,
    override var name: String = "Увеличение скорости атаки"
) : StockEffect() {

    private var stackEffect = 0.0

    override fun applyEffect(current: Person, enemy: Person?, battleObj: BattleObject?) {
        stackEffect -= value
        current.stats.attackSpeed.setEffectValue(AdditionalValue(hashCode().toString(), stackEffect))
    }

    override fun removeEffect(current: Person, enemy: Person?, battleObj: BattleObject?) {
        current.stats.attackSpeed.removeEffectValue(hashCode().toString())
    }
}
@Serializable
class EffectHeal(
    var value: Double,
    override val category: EnumPersonLifects,
    override var name: String = "Восстановление здоровья"
) : StockEffect() {
    override fun applyEffect(current: Person, enemy: Person?, battleObj: BattleObject?) {
        current.stats.health.setEffectValue(AdditionalValue(hashCode().toString(), value))
    }

    override fun removeEffect(current: Person, enemy: Person?, battleObj: BattleObject?) {}
}
@Serializable
class EffectDoubleDamageEveryAttack(
    var everyNAttack: Int,
    override val category: EnumPersonLifects = EnumPersonLifects.ON_BEFORE_DAMAGE,
    override var name: String = "Двойной удар"
) : StockEffect() {
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

@Serializable
class PersonEffects {
    private val arrayEffects = ArrayList<BaseApplyEffect>()

    fun addEffect(effect: BaseApplyEffect) {
        arrayEffects.add(effect)
    }

    fun initForBattle(person: Person) {
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