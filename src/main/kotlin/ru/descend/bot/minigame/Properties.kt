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
        value = obj.toDouble().to1Digits()
    }
    open fun add(obj: Number, invokingListeners: Boolean = true) {
        value = (value + obj.toDouble()).to1Digits()
    }
    open fun rem(obj: Number, invokingListeners: Boolean = true) {
        value = (value - obj.toDouble()).to1Digits()
    }

    override fun toString(): String {
        return "Property(name='$name', value=$value)"
    }
}

open class BattleProperty (
    name: String
) : Property(name) {
    private val arrayOnChange = ArrayList<(BattleProperty) -> Unit>()
    fun addOnChangeListener(body: (BattleProperty) -> Unit) {
        arrayOnChange.add(body)
    }

    private val arrayOnAdd = ArrayList<(BattleProperty) -> Unit>()
    fun addOnAddListener(body: (BattleProperty) -> Unit) {
        arrayOnAdd.add(body)
    }

    private val arrayOnRem = ArrayList<(BattleProperty) -> Unit>()
    fun addOnRemListener(body: (BattleProperty) -> Unit) {
        arrayOnRem.add(body)
    }

    override fun set(obj: Number, invokingListeners: Boolean) {
        super.set(obj, invokingListeners)
        if (!invokingListeners) return
        arrayOnChange.forEach{ i -> i.invoke(this) }
    }
    override fun add(obj: Number, invokingListeners: Boolean) {
        super.add(obj, invokingListeners)
        if (!invokingListeners) return
        arrayOnAdd.forEach{ i -> i.invoke(this) }
        arrayOnChange.forEach{ i -> i.invoke(this) }
    }
    override fun rem(obj: Number, invokingListeners: Boolean) {
        super.rem(obj, invokingListeners)
        if (!invokingListeners) return
        arrayOnRem.forEach{ i -> i.invoke(this) }
        arrayOnChange.forEach{ i -> i.invoke(this) }
    }
}