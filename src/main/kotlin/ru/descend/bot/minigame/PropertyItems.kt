package ru.descend.bot.minigame

import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import dev.kord.common.entity.AuditLogChangeKey.*
import org.junit.Test
import ru.descend.bot.printLog
import java.text.DateFormat


enum class EnumItemCategory(nameCategory: String) {
    COMMON("Обычный"),
    UNCOMMON("Необычный"),
    RARE("Редкий")
}

data class ItemsConstants(
    var isSellable: Boolean = true,
    var isSellableOthers: Boolean = true,
    var isQuests: Boolean = true,
)

data class ObjectsConstants(
    var isEquipped: Boolean = false
)

open class BaseItem (
    val name: String,
    var price: Double = 0.0,
    var constItem: ItemsConstants = ItemsConstants(),
    val constObject: ObjectsConstants = ObjectsConstants(),
    val properties: DataProperties = DataProperties(false),
    var category: EnumItemCategory = EnumItemCategory.COMMON
)

open class BasePerson (
    val name: String,
    val properties: DataProperties = DataProperties(true)
)

class ObjectItem (
    name: String,
) : BaseItem(name)

class Person(
    name: String,
    var arrayItems: ArrayList<BaseItem> = ArrayList(),
) : BasePerson(name) {

    fun calcProperty() : DataProperties {
        val tempProperty = properties.copy()
        arrayItems.forEach {
            if (it is ObjectItem) {
                tempProperty.addProperties(it.properties)
            }
        }
        return tempProperty
    }
}

class TestClass {
    @Test
    fun test_check() {

        val person = Person("Человек Павук")
        person.properties.strength.value = 50.0
        person.properties.strength.globalPercent = 20.0
//        person.property.strength.localPercent = 10.0

        var weaponItem = ObjectItem("Меч Силы")
        weaponItem.properties.strength.value = 20.0
        weaponItem.properties.strength.localPercent = 50.0
        weaponItem.properties.strength.globalPercent = 10.0
        person.arrayItems.add(weaponItem)

        printLog("WEAPON: " + weaponItem.properties.strength)

        printLog("PERSON STOCK: " + person.properties.strength)
        printLog("PERSON CALC: " + person.calcProperty().strength)
    }

    @Test
    fun test_gson() {

        val gson = GsonBuilder()
            .registerTypeAdapter(Property::class.java, TypeAdapterProperty())
            .registerTypeAdapter(PropertyValue::class.java, TypeAdapterPropertyValue())
            .enableComplexMapKeySerialization()
            .serializeNulls()
            .setDateFormat(DateFormat.LONG)
            .setPrettyPrinting()
            .create()

        val data = DataProperties(false)
        data.strength.value = 5.0
        data.level.value = 3.0

        printLog("Object before: $data")
        val text = gson.toJson(data)
        printLog("Result: $text")
        printLog("Object after: ${gson.fromJson(text, DataProperties::class.java)}")
    }
}

class TypeAdapterProperty : TypeAdapter<Property>() {
    private val stringChar = "|"
    override fun write(out: JsonWriter, value: Property) {
        val valueString = value.name +
                stringChar + value.gsonClassCode +
                stringChar + value.gsonCategoryCode +
                stringChar + value.value
        out.value(valueString)
    }
    override fun read(`in`: JsonReader): Property {
        val valueStringObject = `in`.nextString()
        val splittedObject = valueStringObject.split(stringChar)
        val name = splittedObject[0]
        val gsonClassCode = splittedObject[1].toInt()
        val gsonCategoryCode = splittedObject[2].toInt()
        val value = splittedObject[3].toDouble()
        return Property(name, gsonClassCode, gsonCategoryCode, value)
    }
}

class TypeAdapterPropertyValue : TypeAdapter<PropertyValue>() {
    private val stringChar = "|"
    override fun write(out: JsonWriter, value: PropertyValue) {
        val valueString = value.name +
                stringChar + value.gsonCategoryCode +
                stringChar + value.isUnit +
                stringChar + value.value +
                stringChar + value.localPercent +
                stringChar + value.globalPercent
        out.value(valueString)
    }
    override fun read(`in`: JsonReader): PropertyValue {
        val valueStringObject = `in`.nextString()
        val splittedObject = valueStringObject.split(stringChar)
        val name = splittedObject[0]
        val gsonCategoryCode = splittedObject[1].toInt()
        val isUnit = splittedObject[2].toBoolean()
        val value = splittedObject[3].toDouble()
        val localPercent = splittedObject[4].toDouble()
        val globalPercent = splittedObject[5].toDouble()
        val returnedValue = PropertyValue(name, gsonCategoryCode, isUnit, localPercent = localPercent, globalPercent = globalPercent)
        returnedValue.value = value
        return returnedValue
    }
}