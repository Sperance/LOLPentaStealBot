package ru.descend.bot.minigame

import ru.descend.bot.to1Digits

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
)

open class Property (
    name: String
) : BaseProperty(name, 0.0) {
    open fun get() = value
    open fun set(obj: Number, invokingListeners: Boolean = true) {
        change(obj)
    }
    open fun add(obj: Number, invokingListeners: Boolean = true) {
        change(value + obj.toDouble())
    }
    open fun rem(obj: Number, invokingListeners: Boolean = true) {
        change(value - obj.toDouble())
    }
    open fun change(newValue: Number) {
        value = newValue.toDouble().to1Digits()
    }

    override fun toString(): String {
        return "Property(name='$name', value=$value)"
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

class BattleProperty (
    name: String
) : Property(name) {
    val onChangeListeners = BaseListener<BattleProperty>()
    val onAddListeners = BaseListener<BattleProperty>()
    val onRemoveListeners = BaseListener<BattleProperty>()

    var minimumValue = 0.0
    var maximumValue = 0.0

    override fun set(obj: Number, invokingListeners: Boolean) {
        super.set(obj, invokingListeners)
        if (!invokingListeners) return
        onChangeListeners.invokeEach(this)
    }
    override fun add(obj: Number, invokingListeners: Boolean) {
        super.add(obj, invokingListeners)
        if (!invokingListeners) return
        onAddListeners.invokeEach(this)
        onChangeListeners.invokeEach(this)
    }
    override fun rem(obj: Number, invokingListeners: Boolean) {
        super.rem(obj, invokingListeners)
        if (!invokingListeners) return
        onRemoveListeners.invokeEach(this)
        onChangeListeners.invokeEach(this)
    }
}