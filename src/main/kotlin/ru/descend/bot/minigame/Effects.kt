package ru.descend.bot.minigame

open class BaseEffect(
    val name: String,
    val category: EnumPersonLifects,
    var value: Double,
    var duration: Long
) {
    open fun applyEffect(current: Person?, enemy: Person?){
        println("[Effect $name applied]")
    }
    open fun removeEffect(current: Person?, enemy: Person?){
        println("[Effect $name removed]")
    }
}

class BaseEffectAdditionalDamage(value: Double, duration: Long = -1) : BaseEffect("Бонусный урон", EnumPersonLifects.ON_START_BATTLE, value, duration) {
    override fun applyEffect(current: Person?, enemy: Person?) {
        super.applyEffect(current, enemy)
        current?.stats?.attack?.battleStat?.add(value)
    }
    override fun removeEffect(current: Person?, enemy: Person?) {
        super.removeEffect(current, enemy)
        current?.stats?.attack?.battleStat?.rem(value)
    }
}

data class PersonEffects (
    val person: Person
) {
    private val arrayEffects = ArrayList<BaseEffect>()

    fun addEffect(effect: BaseEffect) {
        arrayEffects.add(effect)
    }

    fun initForBattle() {
        person.listeners.onStartBattle.addListener { person1, person2 ->
            arrayEffects.filter { it.category == person.listeners.onStartBattle.category }.forEach {
                it.applyEffect(person1, person2)
            }
        }
    }
}