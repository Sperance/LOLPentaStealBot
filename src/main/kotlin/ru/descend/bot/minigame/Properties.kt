package ru.descend.bot.minigame

import ru.descend.bot.addPercent

open class BlobProperty(
    val name: String,
    var value: Boolean
) {
    fun set(obj: Boolean) {value = obj}
    fun get() = value
}

open class BaseProperty(
    val name: String,
    var value: Double
) {
    open fun get() = value
}

data class AdditionalValue(val code: String, var value: Double, var percent: Double = 0.0)

open class Property (
    name: String
) : BaseProperty(name, 0.0) {

    val onChangeListeners = BaseListener<Property>()
    val onAddListeners = BaseListener<Property>()
    val onRemoveListeners = BaseListener<Property>()

    var minimumValue: Double = 0.0
    var maximumValue: Double? = null

    private val arrayAdditionals = ArrayList<AdditionalValue>()

    override fun get(): Double {
        var result = value
        var sumPercent = 0.0
        arrayAdditionals.forEach {
            result += it.value
            sumPercent += it.percent
        }
        return result.addPercent(sumPercent)
    }

    private fun checkMinMax() {
        if (value < minimumValue) value = minimumValue
        if (maximumValue != null && value > maximumValue!!) value = maximumValue!!
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

    open fun setStock(obj: Number, invokingListeners: Boolean = true) {
        change(obj.toDouble())
        if (!invokingListeners) return
        onChangeListeners.invokeEach(this)
    }
    open fun addStock(obj: Number, invokingListeners: Boolean = true) {
        change(value + obj.toDouble())
        if (!invokingListeners) return
        onAddListeners.invokeEach(this)
        onChangeListeners.invokeEach(this)
    }
    open fun remStock(obj: Number, invokingListeners: Boolean = true) {
        change(value - obj.toDouble())
        if (!invokingListeners) return
        onRemoveListeners.invokeEach(this)
        onChangeListeners.invokeEach(this)
    }

    open fun change(newValue: Number) {
        value = newValue.toDouble()
        checkMinMax()
    }

    override fun toString(): String {
        return "Property(name=$name, value=$value minimumValue=$minimumValue, maximumValue=$maximumValue, additionals=$arrayAdditionals)"
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