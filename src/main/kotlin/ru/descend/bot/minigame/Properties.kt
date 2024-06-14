package ru.descend.bot.minigame

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import ru.descend.bot.addPercent

@Serializable
open class BlobProperty(
    val name: String,
    var value: Boolean
) {
    fun set(obj: Boolean) {value = obj}
    fun get() = value
}

@Serializable
sealed class BaseProperty {

    abstract val name: EnumPropName
    abstract var value: Double
    abstract var innerName: String?
    abstract var stockPercent: Double
    abstract var itemPercent: Double

    open fun get() = value.addPercent(itemPercent)
    override fun toString(): String {
        return "BaseProperty(name=$name, value=$value, stockPercent=$stockPercent, itemPercent=$itemPercent, asItem:${value.addPercent(itemPercent)}, asPerson:${value.addPercent(stockPercent)})"
    }
}

@Serializable
open class StockProperty (
    override val name: EnumPropName = EnumPropName.UNDEFINED,
    override var innerName: String? = null,
    override var value: Double = 0.0,
    override var stockPercent: Double = 0.0,
    override var itemPercent: Double = 0.0
) : BaseProperty()

enum class EnumPropName(val nameProperty: String) {
    UNDEFINED(""),
    HEALTH("Здоровье"),
    MANA("Мана"),
    ENERGY("Энергия"),
    ATTACK("Атака"),
    ATTACK_SPEED("Скорость атаки"),
}

data class AdditionalValue(val code: String, var value: Double, var percent: Double = 0.0)

@Serializable
open class Property (
    override val name: EnumPropName,
    override var innerName: String? = null,
    override var value: Double = 0.0,
    override var stockPercent: Double = 0.0,
    override var itemPercent: Double = 0.0
) : BaseProperty() {

    @Transient private val arrayAdditionals = ArrayList<AdditionalValue>()

    @Transient var maximumValue: Double? = null
    @Transient var minimumValue: Double = 0.0
    @Transient val onChangeListeners = BaseListener<Property>()
    open fun initForBattle(person: Person) {}

    override fun get(): Double {
        var result = value
        var sumPercent = 0.0
        arrayAdditionals.forEach {
            result += it.value
            sumPercent += it.percent
        }
        val finalResult = result.addPercent(stockPercent + sumPercent)
        return checkMinMax(finalResult)
    }

    private fun checkMinMax(obj: Double) : Double {
        if (obj <= minimumValue) return minimumValue
        if (maximumValue != null && obj > maximumValue!!) return maximumValue!!
        return obj
    }

    fun setEffectValue(obj: AdditionalValue) {
        var isSetted = false
        arrayAdditionals.forEach {
            if (it.code == obj.code) {
                it.value = obj.value
                it.percent = obj.percent
                isSetted = true
                return@forEach
            }
        }
        if (!isSetted) arrayAdditionals.add(obj)
    }
    fun addEffectValue(obj: AdditionalValue) {
        var isSetted = false
        arrayAdditionals.forEach {
            if (it.code == obj.code) {
                it.value += obj.value
                it.percent += obj.percent
                isSetted = true
                return@forEach
            }
        }
        if (!isSetted) arrayAdditionals.add(obj)
    }
    fun removeEffectValue(effectCode: String) {
        arrayAdditionals.removeIf { it.code == effectCode }
    }

    fun addForItem(prop: BaseProperty) {
        this.value += prop.get()
        this.stockPercent += prop.stockPercent
    }

    open fun setStock(obj: Number, invokeListener: Boolean = true) {
        change(obj.toDouble(), invokeListener)
    }
    open fun addStock(obj: Number, invokeListener: Boolean = true) {
        change(value + obj.toDouble(), invokeListener)
    }
    open fun remStock(obj: Number, invokeListener: Boolean = true) {
        change(value - obj.toDouble(), invokeListener)
    }

    open fun change(newValue: Number, invokeListener: Boolean = true) {
        value = newValue.toDouble()
        if (invokeListener) onChangeListeners.invokeEach(this)
    }

    override fun toString(): String {
        return "Property(name=$name, get=${get()} value=$value, maximumValue=$maximumValue, percent=$stockPercent, additionals=$arrayAdditionals)"
    }
}

class BaseListener <T> {
    private val arrayListeners = ArrayList<(T) -> Unit>()
    fun addListener(body: (T) -> Unit) {
        arrayListeners.add(body)
    }
    fun invokeEach(obj: T) {
        arrayListeners.forEach { it.invoke(obj) }
    }
}