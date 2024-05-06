package ru.descend.bot.minigame

fun Double.addPercent(percent: Double) : Double {
    return this + (this / 100 * percent)
}

open class Property<T: Number> (
    val name: String,
    var value: T,
) {
    open fun get() = value
    open fun add(obj: Property<T>) {}
    open fun isInitialized() = true
    override fun toString(): String {
        return "Property(name='$name', value=$value, get=${get()})"
    }
}

data class PropertiesPerson(
    val level: PropertyValue = PropertyValue("Уровень")
)
data class PropertiesCalculablePerson(
    val health: PropertyStatGlobal = PropertyStatGlobal("Здоровье"),
    val attackDamage: PropertyStatGlobal = PropertyStatGlobal("Урон"),
    val attackSpeed: PropertyStatGlobal = PropertyStatGlobal("Скорость атаки"),
    val strength: PropertyStatGlobal = PropertyStatGlobal("Сила")
) {
    fun initForBattle() {
        health.initForBattle()
        attackDamage.initForBattle()
        attackSpeed.initForBattle()
        strength.initForBattle()
    }
}

data class PropertiesItem(
    val level: PropertyValue = PropertyValue("Уровень"),
    val price: PropertyValue = PropertyValue("Стоимость"),
)
data class PropertiesCalculableItem(
    val health: PropertyStatLocal = PropertyStatLocal("Здоровье"),
    val strength: PropertyStatLocal = PropertyStatLocal("Сила")
)

open class PropertyValue(
    name: String,
) : Property<Int>(name, value = 0) {
    override fun add(obj: Property<Int>) {
        value += obj.value
    }
    override fun isInitialized(): Boolean {
        return true
    }
}

open class PropertyStatLocal(
    name: String,
    var localPercent: Double = 0.0,
    var globalPercent: Double = 0.0,
) : Property<Double>(name, value = 0.0)  {

    override fun get() : Double {
        return value.addPercent(localPercent)
    }

    override fun isInitialized(): Boolean {
        return value != 0.0 || localPercent != 0.0
    }

    override fun add(obj: Property<Double>) {
        value += obj.get()
    }

    override fun toString(): String {
        if (!isInitialized()) return "<Not init>"
        return "PropertyStatLocal(name='$name', value=$value, local=$localPercent, global=$globalPercent, get=${get()})"
    }
}

open class PropertyStatGlobal(
    name: String,
    var percent: Double = 0.0
) : Property<Double>(name, value = 0.0)  {

    private var battleValue: Double = 0.0
    fun initForBattle() {
        battleValue = get()
    }

    fun getForBattle() = battleValue
    fun removeForBattle(value: Double) {
        battleValue -= value
    }
    fun setForBattle(value: Double) {
        battleValue = value
    }

    override fun get() : Double {
        return value.addPercent(percent)
    }

    override fun isInitialized(): Boolean {
        return value != 0.0 || percent != 0.0
    }

    override fun add(obj: Property<Double>) {
        value += obj.get()
        if (obj is PropertyStatLocal) {
            percent += obj.globalPercent
        }
    }

    override fun toString(): String {
        if (!isInitialized()) return "<Not init>"
        return "PropertyStatGlobal(name='$name', value=$value, percent=$percent, get=${get()})"
    }
}