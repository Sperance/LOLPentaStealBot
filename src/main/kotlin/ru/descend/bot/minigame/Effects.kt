package ru.descend.bot.minigame

abstract class BaseEffect(
    var name: String
) { abstract fun applyEffect(current: Person, enemy: Person?) }

open class BaseApplyEffect(name: String, val listEffects: Collection<BaseEffect>, val codeEffect: Int, val category: EnumPersonLifects) : BaseEffect(name) {
    override fun applyEffect(current: Person, enemy: Person?) {
        listEffects.forEach { it.applyEffect(current, enemy) }
    }
}

/********************************/

class BaseEffectAdditionalDamage(var value: Double) : BaseEffect("Бонусный урон") {
    override fun applyEffect(current: Person, enemy: Person?) {
        current.stats.attack.addStock(value)
    }
}
class BaseEffectAttackSpeedUP(var value: Double) : BaseEffect("Увеличение скорости атаки") {
    override fun applyEffect(current: Person, enemy: Person?) {
        current.stats.attackSpeed.remStock(value)
    }
}
class BaseEffectTimeHeal(var value: Double) : BaseEffect("Восстановление здоровья") {
    override fun applyEffect(current: Person, enemy: Person?) {
        current.stats.health.addStock(value)
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
        person.listeners.onStartBattle.addListener { person1, person2 ->
            arrayEffects.filter { it.category == person.listeners.onStartBattle.category }.forEach { it.applyEffect(person1, person2) }
        }
        person.listeners.onDealDamage.addListener { person1, person2 ->
            arrayEffects.filter { it.category == person.listeners.onDealDamage.category }.forEach { it.applyEffect(person1, person2) }
        }
        person.listeners.onTakeDamage.addListener { person1, person2 ->
            arrayEffects.filter { it.category == person.listeners.onTakeDamage.category }.forEach { it.applyEffect(person1, person2) }
        }
        person.listeners.onDie.addListener { person1, person2 ->
            arrayEffects.filter { it.category == person.listeners.onDie.category }.forEach { it.applyEffect(person1, person2) }
        }
    }
}