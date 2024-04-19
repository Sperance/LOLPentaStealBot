package ru.descend.bot.minigame

fun Double.addPercent(percent: Double) : Double {
    return this + (this / 100 * percent)
}

data class DataProperties(
    val isUnit: Boolean,
    val strength: PropertyValue = PropertyValue("Сила", 1, isUnit),
    val level: Property = PropertySimple("Уровень", 1),
) {
    fun addProperties(obj: DataProperties) : DataProperties {
        DataProperties::class.java.declaredFields.forEach {
            when (val getting = it.get(this)) {
                is PropertyValue -> {
                    getting.add(it.get(obj) as PropertyValue)
                }
                is PropertyBonus -> {
                    getting.add(it.get(obj) as PropertyBonus)
                }
                is Property -> {
                    getting.add(it.get(obj) as Property)
                }
            }
        }
        return this
    }
}

interface IntProperty {
    fun get() : Double
    fun add(obj: Property)
}

open class Property(
    val name: String,
    @Transient
    val gsonClassCode: Int,
    @Transient
    val gsonCategoryCode: Int,
    var value: Double = 0.0,
) : IntProperty {

    override fun get() : Double {
        return value
    }

    override fun add(obj: Property) {
        this.value += obj.value
    }

    override fun toString(): String {
        return "Property(name='$name', value=$value, get=${get()})"
    }
}

class PropertySimple(
    name: String,
    gsonCategoryCode: Int,
) : Property(name, gsonClassCode = 0, gsonCategoryCode = gsonCategoryCode, value = 0.0)

class PropertyBonus(
    name: String,
    gsonCategoryCode: Int,
) : Property(name, gsonClassCode = 2, gsonCategoryCode = gsonCategoryCode, value = 0.0)

class PropertyValue(
    name: String,
    gsonCategoryCode: Int,
    var isUnit: Boolean,
    var localPercent: Double = 0.0,
    var globalPercent: Double = 0.0
) : Property(name, gsonClassCode = 1, gsonCategoryCode = gsonCategoryCode, value = 0.0)  {

    override fun get() : Double {
        return if (isUnit) value.addPercent(globalPercent)
        else value.addPercent(localPercent)
    }

    override fun add(obj: Property) {
        this.value += obj.get()
        if (obj is PropertyValue) {
            this.globalPercent += obj.globalPercent
        }
    }

    override fun toString(): String {
        return "PropertyValue(name='$name', value=$value, localPercent=$localPercent, globalPercent=$globalPercent, get=${get()})"
    }
}