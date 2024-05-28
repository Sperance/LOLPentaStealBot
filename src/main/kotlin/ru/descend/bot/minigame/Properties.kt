package ru.descend.bot.minigame

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

open class Property (
    name: String
) : BaseProperty(name, 0.0) {

    val onChangeListeners = BaseListener<Property>()
    val onAddListeners = BaseListener<Property>()
    val onRemoveListeners = BaseListener<Property>()

    var minimumValue: Double = 0.0
    var maximumValue: Double? = null
    var additionalValue: Double = 0.0

    override fun get(): Double {
        return value + additionalValue
    }

    private fun checkMinMax() {
        if (value < minimumValue) value = minimumValue
        if (maximumValue != null && value > maximumValue!!) value = maximumValue!!
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
        return "Property(name=$name, value=$value minimumValue=$minimumValue, maximumValue=$maximumValue, additionalValue=$additionalValue)"
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

class PersonListener (val name: String, val category: EnumPersonLifects) {
    private val arrayListeners = ArrayList<(Person, Person?) -> Unit>()
    fun addListener(body: (Person, Person?) -> Unit) {
        arrayListeners.add(body)
    }
    fun invokeEach(cur: Person, enemy: Person?) {
        arrayListeners.forEach { it.invoke(cur, enemy) }
    }
}